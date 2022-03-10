import { Overlay } from '@angular/cdk/overlay';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import * as dagre from 'dagre';
import * as graphlib from 'graphlib';
import { dia, layout } from 'jointjs';
import { ModalOptions, NzModalRef, NzModalService } from 'ng-zorro-antd/modal';
import { BehaviorSubject, Subject } from 'rxjs';
import { auditTime, distinctUntilChanged, pairwise, skip } from 'rxjs/operators';
import {
  Class,
  CombinedModel, DataProperty,
  DeltaModification,
  Instance,
  Literal,
  NamedEntity,
  ObjectProperty,
  PrimitiveNode,
  Relation,
  SemanticModel,
  SemanticModelNode,
  SyntaxModel
} from '../../api/generated/dms';
import { ContextMenuService } from '../context-menu/context-menu.service';
import { PlsEntityConceptMenuComponent } from '../context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from '../context-menu/relation-concept-menu/relation-concept-menu.component';
import { ModalMouseEnabledConfig } from '../dialogs/common/modal.base-config';
import {
  PlsSemanticNodeDetailsComponent,
  PlsSemanticNodeDetailsData
} from '../dialogs/semantic-node-dialog/semantic-node.component';
import { PlsSyntaxNodeDetailsComponent } from '../dialogs/syntax-node-dialog/syntax-node.component';
import { Filter } from '../model/common/filter';
import { Graphs } from '../model/configuration/graph';
import { Icons } from '../model/configuration/icons';
import { Links } from '../model/configuration/link';
import { NodeType, Nodes, SemanticNode, SyntaxNode } from '../model/configuration/node';
import { Papers } from '../model/configuration/paper';
import { NodeChangedEvent } from '../model/events/node-changed-event';
import { ModelingService } from '../modeling.service';
import { v4 as uuidv4 } from 'uuid';
import svgPanZoom from '@dash14/svg-pan-zoom';


@Component({
  selector: 'pls-modeling-diagram',
  templateUrl: './diagram.component.html',
  styleUrls: ['./diagram.component.less'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlsDiagramComponent implements OnInit, AfterViewInit {

  // create graph if input schema changes and graph has already been initialized

  @Input() set schema(schema: CombinedModel) {
    this._schema = schema;
    if (!this.initializing) {
      this.createGraph(this.$flat.getValue());
    }
  }

  get schema(): CombinedModel {
    return this._schema;
  }

  @Input() dataSource: string;
  // external controls
  @Input() $center: Subject<any>;
  @Input() $layout: Subject<any>;
  @Input() $filter: BehaviorSubject<Filter>;
  @Input() $flat: BehaviorSubject<boolean>;
  @Input() lockDragAndDrop: boolean;
  // config
  @Input() primaryColor;
  @Input() primaryColorContrast;
  @Input() accentColor;
  @Input() accentColorContrast;
  // data
  initializing = true;
  _schema: CombinedModel;

  // graph elements
  @ViewChild('graph') canvas: ElementRef;
  private graph: dia.Graph;
  private paper: dia.Paper;
  private panAndZoom: any;
  private $resize: BehaviorSubject<{ width: number, height: number }>;

  // dialog
  private nodeDialogRef: NzModalRef<any>;

  // modeling
  modelNode: SemanticModelNode; // currently selected node template
  relation: Relation; // currently selected relation template
  relationTarget = false;
  hoveredElement;
  mousePosition;
  recommendations = [];

  // events
  @Output() graphLaidOut = new EventEmitter<CombinedModel>();
  @Output() semanticModelElementsAdded = new EventEmitter<{ nodes?: Array<SemanticModelNode>, relations?: Array<Relation> }>();
  @Output() semanticModelElementsRemoved = new EventEmitter<{ nodes?: Array<SemanticModelNode>, relations?: Array<Relation> }>();
  @Output() nodeChanged = new EventEmitter<NodeChangedEvent>();
  @Output() nodePositionChanged = new EventEmitter<NodeChangedEvent>();
  @Output() nodeClicked = new EventEmitter<string>();
  @Output() recommendationAccepted = new EventEmitter<DeltaModification>();
  @Output() recomendationRejected = new EventEmitter<DeltaModification>();

  // hidden canvas to calculate label widths
  private _canvas = document.createElement('canvas');

  constructor(private overlay: Overlay, private modelingService: ModelingService, private contextMenu: ContextMenuService, private viewContainerRef: ViewContainerRef, private modal: NzModalService) {
  }

  /**
   * Init everything
   */
  ngOnInit(): void {
    // init joint.js graph
    this.graph = Graphs.createGraph();
    // init joint.js paper
    this.paper = Papers.createPaper(this.graph, () => this.relation, () => this.colorToHex('primary'), this.lockDragAndDrop);
    // create nodes and links
    this.createGraph();
    // subscribe to relation tool
    this.modelingService.relation.subscribe((relationSelection) => {
      this.relation = relationSelection;
    });
    // show / hide recommendations
    this.modelingService.recommendation.subscribe((recommendation) => {
      if (recommendation) {
        this.showRecommendation(recommendation);
      } else {
        this.hideRecommendations();
      }
    });
    // subscribe to external events
    this.$center.subscribe(() => this.center());
    this.$layout.subscribe(() => this.layout());
    this.$filter.pipe(
      skip(1),
      distinctUntilChanged()
    ).subscribe(() => {
      this.createGraph();
    });
    this.$flat.pipe(
      skip(1),
      distinctUntilChanged()
    ).subscribe((flat) => {
      this.createGraph(flat);
    });
  }

  /**
   * Append graph to canvas and register events
   */
  ngAfterViewInit(): void {
    this.canvas.nativeElement.append(this.paper.el);
    this.paper.unfreeze();

    // init pan and zoom
    this.panAndZoom = svgPanZoom(this.canvas.nativeElement.firstChild.childNodes[2],
      {
        viewportSelector: this.canvas.nativeElement.firstChild.childNodes[2].childNodes[1],
        fit: false,
        zoomScaleSensitivity: 0.2,
        panEnabled: false,
        maxZoom: 5,
        minZoom: 0.0001,
        dblClickZoomEnabled: false
      });

    this.panAndZoom.setOnPan(() => {
      this.contextMenu.close(null);
      this.unhighlightAll();
    });
    this.panAndZoom.setOnZoom(() => {
      this.contextMenu.close(null);
      this.unhighlightAll();
    });

    this.paper.on({
      // add selection for nodes
      'element:pointerclick': (elementView: dia.ElementView) => {
        this.unhighlightAll();
        elementView.highlight();
        const model = elementView.model.attributes;
        if (!this.lockDragAndDrop) {
          this.openDetails(model.id, model.type);
        }
      },
      // emit node id
      'element:pointerdown': (elementView: dia.ElementView) => {
        const model = elementView.model.attributes;
        this.nodeClicked.emit(model.id);
      },
      // adjust edges and persist element position on pointer up
      'element:pointerup': (elementView: dia.ElementView) => {
        Links.adjustVertices(this.graph, elementView);
        const element = elementView.model;
        this.updatePosition(element, true);
      },
      'cell:mouseover': () => {
      },
      // Highlights for modeling
      'element:mouseover': (elementView: dia.ElementView) => {
        if (this.modelNode?._class === 'Class' && this.getIfLeafNode(elementView)) {
          elementView.highlight();
          this.hoveredElement = elementView;
        }

        /*
        // TODO only show recommendations if hovered for 0,5s
        const nodeRecommendations = this.filterRecommendationsForAnchor(elementView.model.id as string);
        console.log('mouseover', elementView.model.id, this.schema.recommendations, nodeRecommendations);
        this.showRecommendations(nodeRecommendations);
        console.log('layout elements', this.recommendations.concat(elementView).filter(cell => cell.attributes?.type === Nodes.semanticNodeName));
        const bbox = elementView.getBBox().clone();
        console.log('Root element ' + bbox.x + '/' + bbox.y);
        */

      },
      'element:mouseout': () => {
        if (this.modelNode) {
          this.unhighlightAll();
        }
        this.hideRecommendations();
        this.hoveredElement = null;
      },
      // handle linking of nodes
      'link:connect': (linkView: dia.LinkView) => {
        console.log('link:connect', linkView);
        if (this.relation?._class === 'ObjectProperty') {
          const model = linkView.model.attributes;
          const from = model.source.id;
          const to = model.target.id;
          const relation: ObjectProperty = {
            _class: 'ObjectProperty',
            from,
            to,
            label: this.relation.label,
            uri: this.relation.uri
          };
          this.semanticModelElementsAdded.emit({relations: [relation]});
        }
      },
      // remove highlight and close dialog if clicked in blank zone
      'blank:pointerclick': () => {
        this.unhighlightAll();
        this.modelingService.relation.next(null);
      },
      // Enable pan when mouse button down in blank zone
      'blank:pointerdown': () => {
        this.panAndZoom.enablePan();
      },
      // Disable pan when the mouse button is released
      'cell:pointerup': () => {
        this.panAndZoom.disablePan();
      },
      'link:pointerup': (link, evt, x, y) => {
        console.log('link:pointerup', link, 'target', this.relationTarget, 'event', evt);
        if (this.relation?._class === 'DataProperty') {
          const source = link.sourceView?.model?.id;
          const literal: Literal = {
            _class: 'Literal',
            x,
            y,
            value: '',
            uuid: uuidv4()
          };
          const from = source;
          const to = literal.uuid;
          const relation: DataProperty = {
            _class: 'DataProperty',
            from,
            to,
            label: this.relation.label,
            uri: this.relation.uri
          };
          console.log('ADD DATAPROPERTY', 'source', source, 'literal', literal, 'relation', relation);
          this.semanticModelElementsAdded.emit({nodes: [literal], relations: [relation]});
        } else {
          this.panAndZoom.disablePan();
        }
      },
      'cell:pointerdown': (cellView) => {
        const model = cellView.model.attributes;
        // console.log('cell:pointerdown', cellView);
      },
      // register context menu
      'cell:contextmenu': (cellView: dia.CellView) => {
        const id = cellView.model.attributes.id;
        if (Links.isSemanticLink(cellView)) {
          const link = this.schema.semanticModel.edges.find(edge => edge.uuid === id);
          this.contextMenu.open(cellView.el, PlsRelationConceptMenuComponent, this.viewContainerRef, {
            relation: link
          });
        }
        if (Nodes.isSemanticNode(cellView)) {
          const node = this.findSemanticNodeById(id);
          this.unhighlightAll();
          cellView.highlight();
          this.contextMenu.open(cellView.el, PlsEntityConceptMenuComponent, this.viewContainerRef, {
            entity: node,
            syntaxNode: node._class === 'Class' && (node as Class).instance &&
            (node as Class).instance.syntaxNodeUuid ? (node as Class).instance.syntaxNodeUuid : null
          });
        }
        if (Nodes.isSyntaxNode(cellView)) {
          const node = this.findSyntaxNodeById(id);
          this.unhighlightAll();
          cellView.highlight();
          this.contextMenu.open(cellView.el, PlsEntityConceptMenuComponent, this.viewContainerRef, {
            syntaxNode: node
          });
        }
      },
      // init layout after first rendering
      'render:done': () => {
        // turn off this trigger
        this.paper.off('render:done');
        // if there are no coordinates set, we guess its the initial view
        const initial = !this.schema.syntaxModel.nodes[0].x;
        if (initial) {
          setTimeout(() => {
            // on first view layout and center
            this.layout();
            setTimeout(() => this.center(), 500);
          }, 500);
        } else {
          this.center();
        }
        // subscribe to resize
        const size = this.paper.getComputedSize();
        this.$resize = new BehaviorSubject<{ width: number, height: number }>(size);
        this.$resize.pipe(
          // emit event only once every 500ms
          auditTime(100),
          // new and old value
          pairwise()
        ).subscribe((values) => {
            // update sizes in model
            this.panAndZoom.resize();
            // pan by half of diff to let graph remain in relative center
            const widthScale = (values[1].width - values[0].width) / 2;
            const heightScale = (values[1].height - values[0].height) / 2;
            this.panAndZoom.panBy({x: widthScale, y: heightScale});
          }
        );
        this.initializing = false;
      }
    });
  }

  /**
   * Handles entity dropped event
   * @param event dropped event
   */
  entityDropped(event): void {
    // test if valid target
    const node = this.hoveredElement ? this.getIfLeafNode(this.hoveredElement) : null;
    // create new semantic model node
    if (node) {
      const position = this.hoveredElement.model.attributes.position;
      let semanticNode;
      switch (this.modelNode._class) {
        case 'Class':
          const instance: Instance = {
            label: node.label,
            originalLabel: node.label,
            syntaxNodeUuid: node.uuid
          };
          semanticNode = {
            _class: this.modelNode._class,
            uri: this.modelNode.uri,
            label: this.modelNode.label,
            description: (this.modelNode as Class).description,
            xcoordinate: position.x,
            ycoordinate: position.y,
            instance
          };
          break;
        case 'NamedEntity':
          semanticNode = {
            _class: this.modelNode._class,
            uri: this.modelNode.uri,
            label: this.modelNode.label,
            description: (this.modelNode as Class).description,
            xcoordinate: position.x + 10,
            ycoordinate: position.y + 10
          };
          break;
        case 'Literal':
          console.log('Should not happen!');
          break;
      }
      this.semanticModelElementsAdded.emit(semanticNode);
    } else if (event.isPointerOverContainer) { // dropped in blank area
      const localPosition = this.paper.clientToLocalPoint(this.mousePosition.x, this.mousePosition.y);
      let semanticNode;
      switch (this.modelNode._class) {
        case 'Class':
          semanticNode = {
            _class: this.modelNode._class,
            uri: this.modelNode.uri,
            label: this.modelNode.label,
            description: (this.modelNode as Class).description,
            x: localPosition.x,
            y: localPosition.y
          };
          break;
        case 'NamedEntity':
          semanticNode = {
            _class: this.modelNode._class,
            uri: this.modelNode.uri,
            label: this.modelNode.label,
            description: (this.modelNode as Class).description,
            x: localPosition.x,
            y: localPosition.y
          };
          break;
        case 'Literal':
          console.log('Literals are not dropped!');
          break;
      }
      this.semanticModelElementsAdded.emit({nodes: [semanticNode]});
    }
    // clear all even if dropped out of container
    this.modelNode = null;
    this.unhighlightAll();
  }

  /**
   * Setter for entity node
   * @param node - the new node
   */
  setSelectedModelNode(node: SemanticModelNode): void {
    this.modelNode = node;
  }

  /**
   * Track mouse position during entity modeling to find target nodes
   * @param event mouse event
   */
  @HostListener('mousemove', ['$event'])
  onMousemove(event: MouseEvent): void {
    if (this.modelNode) {
      this.mousePosition = {x: event.clientX, y: event.clientY};
    }
  }

  /**
   * Creates nodes and edges from syntax model
   * @param syntaxModel the syntax model
   */
  private createSyntaxGraph(syntaxModel: SyntaxModel): void {
    if (syntaxModel.nodes.length > 0) {
      syntaxModel.nodes.forEach(node => this.addSyntaxNode(node.uuid, node.label, Icons.getIcon(node), node.x, node.y));
      syntaxModel.edges.forEach(edge => this.addSyntaxLink(edge.from, edge.to));
    }
  }

  /**
   * Filters the given list of recommendations to those which have the anchorId set as an anchor.
   * @param anchorId The id to check
   * @return Sublist of matching recommendations for the given node. Empty if none match
   */
  filterRecommendationsForAnchor(anchorId: string): DeltaModification[] {
    return this.schema.recommendations.filter(rec => rec.anchors.includes(anchorId));
  }

  /**
   * Creates nodes and edges for recommendations and adds them to global variable
   * @param recommendations the recommendations
   */
  private showRecommendations(recommendations: DeltaModification[]): void {
    recommendations.forEach(rec => this.showRecommendation(rec));
  }

  /**
   * Fill graph object with current schema data
   * @param flat mode
   */
  private createGraph(flat: boolean = false): void {
    // remove old content
    this.paper.freeze();
    this.graph.clear();
    this.paper.updateViews();
    // create graph based on filter selection
    switch (this.$filter.getValue()) {
      case Filter.ALL:
        // show semantic nodes and links
        this.createSemanticGraph(this.schema.semanticModel, flat);

        // filter syntax nodes
        const map = this.schema.modelMappings;
        const mappedSyntaxNodes = this.schema.syntaxModel.nodes
          .filter(node => this.schema.modelMappings
            .some(mapping => mapping.schemaUuid === node.uuid)
          );
        const hiddenSyntaxNodesIds = mappedSyntaxNodes.map(node => node.uuid);
        const reducedSyntaxModel = {
          root: this.schema.syntaxModel.root,
          nodes: this.schema.syntaxModel.nodes.filter(node => !mappedSyntaxNodes.includes(node)),
          edges: map.map(mapping => {
            return {
              from: mapping.parentUuid,
              to: mapping.classUuid
            };
          }).concat(
            this.schema.syntaxModel.edges
              .filter(edge => !hiddenSyntaxNodesIds.includes(edge.from) && !hiddenSyntaxNodesIds.includes(edge.to))
              .map(edge => {
                return {
                  from: edge.from,
                  to: edge.to
                };
              })
          )
        };
        this.createSyntaxGraph(reducedSyntaxModel);
        break;
      case Filter.SYNTAX:
        // show syntax nodes and links
        this.createSyntaxGraph(this.schema.syntaxModel);
        break;
      case Filter.SEMANTIC:
        // show semantic nodes and links
        this.createSemanticGraph(this.schema.semanticModel, flat);
        break;
      default:
        console.error('Unsupported Filter');
    }
    this.paper.unfreeze();
    // layout links
    this.graph.getLinks().forEach(el => Links.adjustVertices(this.graph, el));
  }

  /**
   * Removes recommendations from graph
   */
  private hideRecommendations(): void {
    this.graph.removeCells(this.recommendations);
    this.recommendations = [];
  }

  /**
   * Creates nodes and edges from semantic model
   * @param semanticModel the semantic model
   * @param flat maps names to url
   */
  private createSemanticGraph(semanticModel: SemanticModel, flat: boolean): void {
    const color = 'primary';
    if (semanticModel.nodes.length > 0) {
      semanticModel.nodes.forEach(node => {
        // console.log('adding node', node);
        if (node._class === 'Class') {
          const instance = (node as Class).instance;
          // get icon from syntax node
          let icon = '';
          if (instance?.syntaxNodeUuid) {
            icon = Icons.getIcon(this.findSyntaxNodeById(instance.syntaxNodeUuid));
          }
          this.addClassNode(node.uuid, flat ? node.uri : node.label, color, node.x, node.y, 1, instance?.label, icon);
        } else if (node._class === 'NamedEntity') {
          this.addNamedEntityNode(node.uuid, flat ? node.uri : node.label, color, node.x, node.y);
        } else if (node._class === 'Literal') {
          this.addLiteralNode(node.uuid, node.label, 'text_fields', color, node.x, node.y);
        }
      });

      semanticModel.edges.forEach(edge => {
        if (edge._class === 'DataProperty') {
          this.addSemanticLink(edge.uuid, edge.from, edge.to, flat ? edge.uri : edge.label, 'highlight');
        } else {
          this.addSemanticLink(edge.uuid, edge.from, edge.to, flat ? edge.uri : edge.label, 'accent');
        }
      });
    }
  }

  /**
   * Add syntax node to graph
   * @param id of node
   * @param label of node
   * @param icon of node
   * @param x of node
   * @param y of node
   */
  private addSyntaxNode(id: string, label: string, icon: string, x?: number, y?: number): void {
    const dim = this.calcDimensionsForLabel(label, !!icon);
    const position = {x, y};
    this.graph.addCell(Nodes.createSyntaxNode(id, label, icon, position, dim[0], dim[1], this.colorToHex('primary')));
  }

  /**
   * Add syntax link to graph
   * @param source of link
   * @param target of link
   */
  private addSyntaxLink(source: string, target: string): void {
    this.graph.addCell(Links.createSyntaxLink(source, target, this.colorToHex('primary')));
  }

  /**
   * Calculate rendered dimensions for given text and icon
   * @param text to test
   * @param icon true if also includes icon
   * @returns [width, height]
   */
  private calcDimensionsForLabel(text: string, icon: boolean): [number, number] {
    const letterSize = 7;
    const iconWidth = icon ? 30 : 0;
    const width = this.getTextWidth(text, '11pt Roboto') + 10 + iconWidth;
    const height = 2 * ((text.split('\n').length + 1) * letterSize);
    return [width, height];
  }

  /**
   * Renders text in canvas and returns rendered width
   * @param text to test
   * @param font font of text
   */
  private getTextWidth(text, font): number {
    // re-use canvas object for better performance
    const context = this._canvas.getContext('2d');
    context.font = font;
    const metrics = context.measureText(text);
    return metrics.width;
  }

  /**
   * Fit and center graph
   */
  private center(): void {
    // update bounding box in case user dragged stuff
    this.panAndZoom.updateBBox();
    // fit and center
    this.panAndZoom.fit();
    this.panAndZoom.center();
    // leave small margin
    this.panAndZoom.zoomBy(0.98);
  }

  /**
   * Lay out graph and persist positions
   */
  private layout(): void {
    // layout top to bottom with spaces: node 50, edge 50, rank (layer) 70
    layout.DirectedGraph.layout(this.graph, {
      dagre,
      graphlib,
      nodeSep: 50,
      edgeSep: 50,
      rankSep: 70,
      rankDir: 'TB'
    });
    // wait short time for rendering to complete
    setTimeout(() => {
      // update x and y values in model
      this.graph.getElements().forEach((element) => {
        this.updatePosition(element, false);
      });
      // update links layout
      setTimeout(() => {
        this.graph.getLinks().forEach(el => Links.adjustVertices(this.graph, el));
      }, 300);
      // persist new positions
      this.graphLaidOut.emit(this.schema);
    }, 500);
  }

  /**
   * Creates nodes and edges from recommendation and adds it to global variable
   * @param recommendation the recommendation
   */
  private showRecommendation(recommendation: DeltaModification): void {
    const opacity = 0.8;
    const color = 'highlight';
    recommendation.entities.forEach(node => {
      let icon = '';
      if (node._class === 'Class') {
        const instance = (node as Class).instance;
        if (instance && instance.syntaxNodeUuid) {
          icon = Icons.getIcon(this.findSyntaxNodeById(instance.syntaxNodeUuid));
        }
        this.recommendations.push(this.addClassNode(node.uuid, node.uri, color, node.x, node.y, opacity, instance?.label, icon));
      }
    });
    recommendation.relations.forEach(edge => this.recommendations.push(this.addSemanticLink(edge.uuid, edge.from, edge.to, edge.label, color, opacity)));
  }

  /**
   * Add semantic link to graph
   */
  private addSemanticLink(id: string, source: string, target: string, label: string, color: 'primary' | 'accent' | 'highlight' = 'primary', opacity?: number): dia.Link {
    const link = Links.createSemanticLink(id, source, target, label, this.colorToHex(color), opacity);
    this.graph.addCell(link);
    console.log('link added', link);
    return link;
  }

  private createConfig(title: string, content: any, data: SyntaxNode | PlsSemanticNodeDetailsData): ModalOptions {
    const rect: DOMRect = this.viewContainerRef.element.nativeElement.getBoundingClientRect();
    return Object.assign({
      nzStyle: {position: 'absolute', top: `${rect.top + 15}px`, right: `15px`},
      nzTitle: title,
      nzContent: content,
      nzComponentParams: {
        data
      }
    }, ModalMouseEnabledConfig);
  }

  /**
   * Closes open dialog
   */
  private closeDialog(): void {
    if (this.nodeDialogRef) {
      this.nodeDialogRef.close();
      this.nodeDialogRef = null;
    }
  }

  /**
   * Closes dialog and removes all highlights from graph
   */
  private unhighlightAll(): void {
    this.closeDialog();
    this.paper.findViewsInArea(this.paper.getArea()).forEach(cell => {
      cell.unhighlight();
    });
  }

  /**
   * Adds a semantic class node to the graph
   */
  private addClassNode(id: string, uri: string, color: 'primary' | 'accent' | 'highlight' = 'primary', x?: number, y?: number, opacity?: number, instancelabel?: string, icon?: string,): dia.Element {
    const classDims = this.calcDimensionsForLabel(uri, false);
    let instanceDims = [0, 0];
    if (instancelabel) {
      instanceDims = this.calcDimensionsForLabel(instancelabel, !!icon);
    }
    const dim = [Math.max(classDims[0], instanceDims[0]), classDims[1] + instanceDims[1]];
    const position = {x, y};
    const nodeColors = {color: this.colorToHex(color), contrast: this.colorToHex(color, true)};
    let node;
    if (instancelabel) {
      node = Nodes.createInstancedSemanticClassNode(id, uri, instancelabel, icon, position, dim[0], dim[1], nodeColors, opacity);
    } else {
      node = Nodes.createSemanticClassNode(id, uri, position, dim[0], dim[1], nodeColors, opacity);
    }
    this.graph.addCell(node);
    return node;
  }

  /**
   * Adds a named entity node to the graph
   */
  private addNamedEntityNode(id: string, label: string, color: 'primary' | 'accent' | 'highlight' = 'primary', x?: number, y?: number, opacity?: number): dia.Element {
    const classDims = this.calcDimensionsForLabel(label, false);
    const dim = [classDims[0], classDims[1]];
    const position = {x, y};
    const nodeColors = {color: this.colorToHex(color), contrast: this.colorToHex(color, true)};
    const node = Nodes.createNamedEntityNode(id, label, position, dim[0], dim[1], nodeColors, opacity);
    this.graph.addCell(node);
    return node;
  }

  /**
   * Adds a literal node to the graph
   */
  private addLiteralNode(id: string, value: string, icon: string, color: 'primary' | 'accent' | 'highlight' = 'primary', x?: number, y?: number, opacity?: number): dia.Element {
    const classDims = this.calcDimensionsForLabel(value, !!icon);
    const dim = [classDims[0], classDims[1]];
    const position = {x, y};
    const nodeColors = {color: this.colorToHex(color), contrast: this.colorToHex(color, true)};
    const node = Nodes.createLiteralNode(id, value, icon, position, dim[0], dim[1], nodeColors, opacity);
    this.graph.addCell(node);
    return node;
  }

  /**
   * Finds syntax node data model for given id
   * @param id of node to find
   */
  private findSyntaxNodeById(id: string): SyntaxNode {
    return this.schema.syntaxModel.nodes.find(node => node.uuid === id);
  }

  /**
   * Finds semantic node data model for given id
   * @param id of node to find
   */
  private findSemanticNodeById(id: string): SemanticNode {
    return this.schema.semanticModel.nodes.find(node => node.uuid === id);
  }

  /**
   * Update positions in data model of element and emit event
   * @param element changed element
   * @param emitEvent - true if emit should be emitted
   */
  private updatePosition(element: dia.Element, emitEvent: boolean): void {
    const model = element.attributes;
    if (Nodes.isSyntaxNode(element)) {
      // update positions in data model
      const node = this.findSyntaxNodeById(model.id);
      const position = model.position;
      if (node && (node.x !== position.x || node.y !== position.y)) {
        node.x = position.x;
        node.y = position.y;
        // emit event
        if (emitEvent) {
          const nodeChanges = new NodeChangedEvent(Nodes.syntaxNodeName, node);
          this.nodePositionChanged.emit(nodeChanges);
        }
      }
    }
    if (Nodes.isSemanticNode(element)) {
      // update positions in data model
      const node = this.findSemanticNodeById(model.id);
      const position = model.position;
      if (node && (node.x !== position.x || node.y !== position.y)) {
        node.x = position.x;
        node.y = position.y;
        // emit event
        if (emitEvent) {
          const nodeChanges = new NodeChangedEvent(Nodes.semanticClassName, node);
          this.nodePositionChanged.emit(nodeChanges);
        }
        // update mapped syntax node position
        if (node._class === 'Class' && (node as Class).instance?.syntaxNodeUuid) {
          // TODO this could be done by the backend
          const mappedNode = this.findSyntaxNodeById((node as Class).instance?.syntaxNodeUuid);
          mappedNode.x = position.x;
          mappedNode.y = position.y;
          if (emitEvent) {
            const nodeChanges = new NodeChangedEvent(Nodes.syntaxNodeName, mappedNode);
            this.nodePositionChanged.emit(nodeChanges);
          }
        }
      }
    }
  }

  /**
   * Searches a syntax node for gi
   * @param elementView search element
   */
  private getIfLeafNode(elementView: dia.ElementView): PrimitiveNode {
    if (Nodes.isSyntaxNode(elementView)) {
      const node = this.findSyntaxNodeById(elementView.model.id.toString());
      if (node._class === 'PrimitiveNode') {
        return node;
      }
    }
    return null;
  }

  /**
   * Show details dialog for node with given id
   * @param id of details
   * @param type of node
   */
  private openDetails(id: string, type: NodeType): void {
    this.closeDialog();
    switch (type) {
      case Nodes.syntaxNodeName:
        const syntaxNode = this.findSyntaxNodeById(id);
        this.nodeDialogRef = this.modal.create(this.createConfig(syntaxNode.label, PlsSyntaxNodeDetailsComponent, syntaxNode));
        this.nodeDialogRef.afterClose.subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
          }
        });
        break;
      case Nodes.semanticClassName:
        const semanticNode = this.findSemanticNodeById(id);
        const title = `${semanticNode.label} (${semanticNode.uri})`;
        this.nodeDialogRef = this.modal.create(this.createConfig(title, PlsSemanticNodeDetailsComponent, {
          modelId: this.dataSource,
          node: semanticNode,
          recommendations: this.filterRecommendationsForAnchor(semanticNode.uuid)
        }));
        const componentInstance: PlsSemanticNodeDetailsComponent = this.nodeDialogRef.componentInstance as PlsSemanticNodeDetailsComponent;
        componentInstance.recommendationSelect.subscribe(recommendation => {
          this.hideRecommendations();
          if (recommendation !== null) {
            this.showRecommendation(recommendation);
          }
        });
        componentInstance.recommendationAccept.subscribe(recommendation => {
          this.hideRecommendations();
          this.recommendationAccepted.emit(recommendation);
        });
        this.nodeDialogRef.afterClose.subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
            const element = this.graph.getElements().find(el => el.attributes.id === id);
            // TODO update width
            element.attr('bodyText/text', semanticNode.label);
          }
        });
        break;
      case Nodes.namedEntityName:
        const namedEntityNode = this.findSemanticNodeById(id);
        const namedEntityTitle = `${namedEntityNode.label} (${namedEntityNode.uri})`;
        this.nodeDialogRef = this.modal.create(this.createConfig(namedEntityTitle, PlsSemanticNodeDetailsComponent, {
          modelId: this.dataSource,
          node: namedEntityNode,
          recommendations: this.filterRecommendationsForAnchor(namedEntityNode.uuid)
        }));
        this.nodeDialogRef.afterClose.subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
            const element = this.graph.getElements().find(el => el.attributes.id === id);
            // TODO update width
            element.attr('bodyText/text', semanticNode.label);
          }
        });
        break;
      case Nodes.literalName:
        const literalNode = this.findSemanticNodeById(id);
        const literalTitle = `${literalNode.label}`;
        this.nodeDialogRef = this.modal.create(this.createConfig(literalTitle, PlsSemanticNodeDetailsComponent, {
          modelId: this.dataSource,
          node: literalNode,
          recommendations: this.filterRecommendationsForAnchor(literalNode.uuid)
        }));
        this.nodeDialogRef.afterClose.subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
            const element = this.graph.getElements().find(el => el.attributes.id === id);
            // TODO update width
            element.attr('bodyText/text', semanticNode.label);
          }
        });
        break;
      default:
        console.error('Unsupported node type');
    }
  }

  /**
   * Listen to window resize events
   */
  @HostListener('window:resize')
  onResize(): void {
    if (this.$resize) {
      this.$resize.next(this.paper.getComputedSize());
    }
  }

  /**
   * Transform primary and accent into hex values
   * @param color to convert
   * @param contrast if contrast
   */
  private colorToHex(color: 'primary' | 'accent' | 'highlight', contrast: boolean = false): string {
    switch (color) {
      case 'primary':
        if (contrast) {
          return '#fff';
        } else {
          return '#000';
        }
      case 'accent':
        if (contrast) {
          return '#fff';
        } else {
          return '#111d4f';
        }
      case 'highlight':
        if (contrast) {
          return '#eb973f';
        } else {
          return '#eb973f';
        }
    }
  }

}
