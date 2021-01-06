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
  ViewContainerRef
} from '@angular/core';
import { dia, layout, shapes } from 'jointjs';
import { BehaviorSubject, Subject } from 'rxjs';
import * as dagre from 'dagre';
import * as graphlib from 'graphlib';
import * as svgPanZoom from 'svg-pan-zoom';
import {
  DataSourceSchemaDTO,
  DeltaRecommendationDTO,
  EntityConceptDTO,
  EntityTypeDTO,
  PrimitiveDTO,
  RelationConceptDTO,
  RelationDTO,
  SemanticModelDTO,
  SyntaxModelDTO
} from '../../api/generated/dms';
import { auditTime, distinctUntilChanged, pairwise, skip } from 'rxjs/operators';
import { Filter } from '../model/common/filter';
import { MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import { PlsSyntaxNodeDetailsComponent } from '../dialogs/syntax-node-dialog/syntax-node.component';
import { PlsSemanticNodeDetailsComponent } from '../dialogs/semantic-node-dialog/semantic-node.component';
import { EntityDroppedEvent } from '../model/events/entity-dropped-event';
import { NodeChangedEvent } from '../model/events/node-changed-event';
import { ModelingService } from '../modeling.service';
import { Overlay } from '@angular/cdk/overlay';
import { ContextMenuService } from '../context-menu/context-menu.service';
import { PlsEntityConceptMenuComponent } from '../context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from '../context-menu/relation-concept-menu/relation-concept-menu.component';
import { plasmaGraph } from '../model/configuration/graph';
import { plasmaPaper } from '../model/configuration/paper';
import { LinkUtil } from '../model/link/link-util';
import { Util } from '../model/common/util';
import { NodeUtil } from '../model/node/node-util';
import { syntaxNode, SyntaxNode } from '../model/node/syntax-node';
import { Icons } from '../model/configuration/icons';
import { semanticNode } from '../model/node/semantic-node';
import { Node, Nodes } from '../model/node/node';


@Component({
  selector: 'pls-modeling-diagram',
  templateUrl: './diagram.component.html',
  styleUrls: ['./diagram.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlsDiagramComponent implements OnInit, AfterViewInit {

  // create graph if input schema changes and graph has already been initialized
  @Input() set schema(schema: DataSourceSchemaDTO) {
    this._schema = schema;
    if (!this.initializing) {
      this.createGraph();
    }
  }

  get schema(): DataSourceSchemaDTO {
    return this._schema;
  }

  @Input() dataSource: string;
  // external controls
  @Input() $center: Subject<any>;
  @Input() $layout: Subject<any>;
  @Input() $filter: BehaviorSubject<Filter>;
  @Input() $flat: BehaviorSubject<boolean>;
  // config
  @Input() primaryColor;
  @Input() primaryColorContrast;
  @Input() accentColor;
  @Input() accentColorContrast;
  // data
  initializing = true;
  _schema: DataSourceSchemaDTO;

  @Input() initial: boolean;

  // graph elements
  @ViewChild('graph') canvas: ElementRef = null;
  private graph: dia.Graph;
  private paper: dia.Paper;
  private panAndZoom: SvgPanZoom.Instance;
  private $resize: BehaviorSubject<{ width: number, height: number }>;

  // dialog
  private nodeDialogRef: MatDialogRef<any>;

  // modeling
  entityConcept;
  relationConcept: RelationConceptDTO;
  hoveredElement;
  mousePosition;
  recommendations = [];

  // events
  @Output() graphLaidOut = new EventEmitter<DataSourceSchemaDTO>();
  @Output() entityConceptMapped = new EventEmitter<EntityDroppedEvent>();
  @Output() relationConceptLinked = new EventEmitter<RelationDTO>();
  @Output() nodeChanged = new EventEmitter<NodeChangedEvent>();
  @Output() nodePositionChanged = new EventEmitter<NodeChangedEvent>();

  // hidden canvas to calculate label widths
  private _canvas = document.createElement('canvas');

  constructor(public dialog: MatDialog, private overlay: Overlay, private modelingService: ModelingService, private contextMenu: ContextMenuService, private viewContainerRef: ViewContainerRef) {
  }

  /**
   * Init everything
   */
  ngOnInit(): void {
    // init joint.js graph
    this.graph = plasmaGraph();
    // init joint.js paper
    this.paper = plasmaPaper(this.graph, () => this.relationConcept);
    // create nodes and links
    this.createGraph();
    // subscribe to relation tool
    this.modelingService.relationConcept.subscribe((relationSelection) => {
      this.relationConcept = relationSelection;
    });
    // show / hide recommendations
    this.modelingService.recommendation.subscribe((recommendation) => {
      if (recommendation) {
        this.showRecommendation(recommendation);
      } else {
        this.hideRecommendation();
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
        minZoom: 0.0001
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
        this.openDetails(model.id, model.type);
      },
      // adjust edges and persist element position on pointer up
      'element:pointerup': (elementView: dia.ElementView) => {
        LinkUtil.adjustVertices(this.graph, elementView);
        const element = elementView.model;
        this.updatePosition(element, true);
      },
      // Highlights for modeling
      'element:mouseover': (elementView: dia.ElementView) => {
        if (this.entityConcept && this.getIfLeafNode(elementView)) {
          elementView.highlight();
          this.hoveredElement = elementView;
        }
      },
      'element:mouseout': () => {
        if (this.entityConcept) {
          this.unhighlightAll();
        }
        this.hoveredElement = null;
      },
      // handle linking of nodes
      'link:connect': (linkView: dia.LinkView) => {
        const model = linkView.model.attributes;
        const from = model.source.id;
        const to = model.target.id;
        const relation: RelationDTO = {
          from,
          to,
          description: this.relationConcept.description,
          concept: this.relationConcept
        };
        this.relationConceptLinked.emit(relation);
      },
      // remove highlight and close dialog if clicked in blank zone
      'blank:pointerclick': () => {
        this.unhighlightAll();
        this.modelingService.relationConcept.next(null);
      },
      // Enable pan when mouse button down in blank zone
      'blank:pointerdown': () => {
        this.panAndZoom.enablePan();
      },
      // Disable pan when the mouse button is released
      'cell:pointerup blank:pointerup': () => {
        this.panAndZoom.disablePan();
      },
      // register context menu
      'cell:contextmenu': (cellView: dia.CellView) => {
        const id = cellView.model.attributes.id;
        if (LinkUtil.isSemanticLink(cellView)) {
          const link = this.schema.semanticModel.edges.find(edge => edge.id === id);
          this.contextMenu.open(cellView.el, PlsRelationConceptMenuComponent, this.viewContainerRef, {
            concept: link
          });
        }
        if (NodeUtil.isSemanticNode(cellView)) {
          const node = this.findSemanticNodeById(id);
          this.unhighlightAll();
          cellView.highlight();
          this.contextMenu.open(cellView.el, PlsEntityConceptMenuComponent, this.viewContainerRef, {
            concept: node
          });
        }
      },
      // init layout after first rendering
      'render:done': () => {
        // turn off this trigger
        this.paper.off('render:done');
        if (this.initial) {
          // on first view layout and center
          this.layout();
          this.initial = false;
          setTimeout(() => this.center(), 100);
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
   * @param event
   */
  entityDropped(event): void {
    // test if valid target
    const node = this.hoveredElement ? this.getIfLeafNode(this.hoveredElement) : null;
    // create entity type
    if (node) {
      const position = this.hoveredElement.model.attributes.position;
      const entityType: EntityTypeDTO = {
        concept: this.entityConcept,
        originalLabel: node.label,
        label: node.label,
        description: this.entityConcept.description,
        x: position.x,
        y: position.y
      };
      this.entityConceptMapped.emit(new EntityDroppedEvent(entityType, node.id));
      // dropped in blank area
    } else if (event.isPointerOverContainer) {
      const localPosition = this.paper.clientToLocalPoint(this.mousePosition.x, this.mousePosition.y);
      const entityType: EntityTypeDTO = {
        concept: this.entityConcept,
        label: this.entityConcept.name,
        description: this.entityConcept.description,
        x: localPosition.x,
        y: localPosition.y
      };
      this.entityConceptMapped.emit(new EntityDroppedEvent(entityType));
    }
    // clear all even if dropped out of container
    this.entityConcept = null;
    this.unhighlightAll();
  }

  /**
   * Fill graph object with current schema data
   * @param flat mode
   * @private
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
        const map = this.schema.primitiveToEntityTypeList;
        const mappedSyntaxNodes = Object.values(map).map(entityMap => entityMap.mappedPrimitive);
        const filteredSyntaxNodes = this.schema.syntaxModel.nodes.filter(node => !mappedSyntaxNodes.includes(node.id));
        const filteredSyntaxNodesIds = filteredSyntaxNodes.map(node => node.id);
        const filteredSyntaxModel = {
          root: this.schema.syntaxModel.root,
          nodes: filteredSyntaxNodes,
          edges: map.concat(...this.schema.syntaxModel.edges.filter(edge => filteredSyntaxNodesIds.includes(edge.from) && filteredSyntaxNodesIds.includes(edge.to)))
        };
        this.createSyntaxGraph(filteredSyntaxModel);
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
    this.graph.getLinks().forEach(el => LinkUtil.adjustVertices(this.graph, el));
  }

  /**
   * Creates nodes and edges from semantic model
   * @param semanticModel
   * @param flat
   * @private
   */
  private createSemanticGraph(semanticModel: SemanticModelDTO, flat: boolean): void {
    if (semanticModel.nodes.length > 0) {
      semanticModel.nodes.forEach(node => {
        let icon = '';
        if (node.mappedToData) {
          // get icon from syntax node
          icon = Icons.getIcon(this.getMappedPrimitive(node.id));
        }
        this.addSemanticNode(node.id, flat ? node.concept.sourceURI : node.concept.name, node.label, icon, 'primary', node.x, node.y);
      });
      semanticModel.edges.forEach(edge => this.addLink(edge.id, edge.from, edge.to, false, flat ? edge.concept.sourceURI : edge.concept.name));
    }
  }

  /**
   * Creates nodes and edges from syntax model
   * @param syntaxModel
   * @private
   */
  private createSyntaxGraph(syntaxModel: SyntaxModelDTO): void {
    if (syntaxModel.nodes.length > 0) {
      syntaxModel.nodes.forEach(node => this.addSyntaxNode(node.id, node.label, Icons.getIcon(node), node.x, node.y));
      syntaxModel.edges.forEach(edge => this.addLink(null, edge.from, edge.to, true));
    }
  }

  /**
   * Creates nodes and edges from recommendation and adds it to global variable
   * @param recommendation
   * @private
   */
  private showRecommendation(recommendation: DeltaRecommendationDTO): void {
    recommendation.entities.forEach(node => {
      let icon = '';
      if (node.mappedToData) {
        icon = Icons.getIcon(this.getMappedPrimitive(node.id));
      }
      this.recommendations.push(this.addSemanticNode(node.id, node.concept.name, node.label, icon, 'accent', node.x, node.y, 0.6));
    });
    recommendation.relations.forEach(edge => this.recommendations.push(this.addLink(edge.id, edge.from, edge.to, false, edge.concept.name, 'accent')));
  }

  /**
   * Removes recommendations from graph
   * @private
   */
  private hideRecommendation(): void {
    this.graph.removeCells(this.recommendations);
    this.recommendations = [];
  }

  /**
   * Adds a semantic node to the graph
   * @param id
   * @param concept
   * @param label
   * @param icon
   * @param color
   * @param x
   * @param y
   * @param opacity
   * @private
   */
  private addSemanticNode(id: string, concept: string, label: string, icon: string, color: 'primary' | 'accent' = 'primary', x?: number, y?: number, opacity?: number): dia.Element {
    const conceptDims = this.calcDimensionsForLabel(concept, false);
    const labelDims = this.calcDimensionsForLabel(label, !!icon);
    const dim = [Math.max(conceptDims[0], labelDims[0]), conceptDims[1] + labelDims[1]];
    const position = {x, y};
    const node: dia.Element = new semanticNode({
      id,
      // @ts-ignore not in jointjs model but works this way
      position,
      size: {
        width: dim[0],
        height: dim[1]
      },
      attrs: {
        body: {
          stroke: this.colorToHex(color),
          opacity: opacity ? opacity : 1,
        },
        header: {
          fill: this.colorToHex(color),
          stroke: this.colorToHex(color),
          opacity: opacity ? opacity : 1
        },
        headerText: {
          text: concept,
          fill: this.colorToHex(color, true),
        },
        bodyIcon: {
          text: icon,
          fill: this.colorToHex(color),
        },
        bodyText: {
          // adjust text for icon
          refX: icon ? '35' : '50%',
          textAnchor: icon ? 'left' : 'middle',
          text: label,
          fill: this.colorToHex(color),
        }
      }
    });
    this.graph.addCell(node);
    return node;
  }

  /**
   * Adds syntax node to graph
   * @param id
   * @param label
   * @param icon
   * @param x
   * @param y
   * @private
   */
  private addSyntaxNode(id: string, label: string, icon: string, x?: number, y?: number): void {
    const dim = this.calcDimensionsForLabel(label, true);
    const position = {x, y};
    const node = new syntaxNode({
      id,
      // @ts-ignore not in jointjs model but works this way
      position,
      size: {
        width: dim[0],
        height: dim[1]
      },
      attrs: {
        body: {
          stroke: this.colorToHex('primary'),
        },
        icon: {
          text: icon,
          fill: this.colorToHex('primary')
        },
        label: {
          text: label,
          fill: this.colorToHex('primary')
        }
      }
    });
    this.graph.addCell(node);
  }

  /**
   * Calculates rendered dimensions for given text and icon
   * @param text
   * @param icon
   * @returns [width, height]
   * @private
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
   * @param text
   * @param font
   * @private
   */
  private getTextWidth(text, font): number {
    // re-use canvas object for better performance
    const context = this._canvas.getContext('2d');
    context.font = font;
    const metrics = context.measureText(text);
    return metrics.width;
  }

  private addLink(id: string, source: string, target: string, syntaxModel: boolean, label?: string, color: 'primary' | 'accent' = 'primary'): any {
    const labels = label ? [{
      attrs: {
        text: {
          text: label,
          fill: this.colorToHex(color)
        }
      },
      position: {
        distance: 0.5,
        offset: (label.indexOf('\n') > -1 || label.length === 1) ? 0 : 10,
        args: {
          keepGradient: true,
          ensureLegibility: true
        }
      }
    }] : undefined;
    const link = new shapes.standard.Link(
      {
        id,
        source: {id: source},
        target: {id: target},
        linkType: syntaxModel ? 'syntax' : 'semantic',
        labels,
        attrs: {
          line: {
            stroke: this.colorToHex(color),
            strokeOpacity: syntaxModel ? 0.75 : 1,
            strokeWidth: 1,
            strokeDasharray: syntaxModel ? '10,2' : undefined
          }
        }
      }
    );
    // link style
    link.router('normal');
    link.connector('jumpover');
    // show in graph
    this.graph.addCell(link);
    return link;
  }

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
   * @private
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
        this.graph.getLinks().forEach(el => LinkUtil.adjustVertices(this.graph, el));
      }, 300);
      // persist new positions
      this.graphLaidOut.emit(this.schema);
    }, 500);
  }

  /**
   * Update positions in data model of element and emit event
   * @param element changed element
   * @param emitEvent - true if emit should be emitted
   * @private
   */
  private updatePosition(element: dia.Element, emitEvent: boolean): void {
    const model = element.attributes;
    if (NodeUtil.isSyntaxNode(element)) {
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
    if (NodeUtil.isSemanticNode(element)) {
      // update positions in data model
      const node = this.findSemanticNodeById(model.id);
      const position = model.position;
      if (node && (node.x !== position.x || node.y !== position.y)) {
        node.x = position.x;
        node.y = position.y;
        // emit event
        if (emitEvent) {
          const nodeChanges = new NodeChangedEvent(Nodes.semanticNodeName, node);
          this.nodePositionChanged.emit(nodeChanges);
        }
        // update mapped syntax node position
        if (node.mappedToData) {
          const mappedNode = this.getMappedPrimitive(node.id);
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
   * Show details dialog for node with given id
   * @param id
   * @param type
   * @private
   */
  private openDetails(id: string, type: Node): void {
    this.closeDialog();
    const config: MatDialogConfig = {
      hasBackdrop: false,
      data: {
        dataSource: this.dataSource,
        node: null,
        relativeTo: this.canvas
      },
      maxHeight: '90%',
      maxWidth: '500px',
      closeOnNavigation: true,
      autoFocus: false
    };
    switch (type) {
      case Nodes.syntaxNodeName:
        config.data.node = this.findSyntaxNodeById(id);
        this.nodeDialogRef = this.dialog.open(PlsSyntaxNodeDetailsComponent, config);
        this.nodeDialogRef.beforeClosed().subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
          }
        });
        break;
      case Nodes.semanticNodeName:
        config.data.node = this.findSemanticNodeById(id);
        this.nodeDialogRef = this.dialog.open(PlsSemanticNodeDetailsComponent, config);
        this.nodeDialogRef.beforeClosed().subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
          }
          const element = this.graph.getElements().find(el => el.attributes.id === id);
          // TODO update width
          element.attr('bodyText/text', config.data.node.label);
        });
        break;
      default:
        console.error('Unsupported node type');
    }
  }

  /**
   * Closes open dialog
   * @private
   */
  private closeDialog(): void {
    if (this.nodeDialogRef) {
      this.nodeDialogRef.close();
      this.nodeDialogRef = null;
    }
  }

  /**
   * Closes dialog and removes all highlights from graph
   * @private
   */
  private unhighlightAll(): void {
    this.closeDialog();
    this.paper.findViewsInArea(this.paper.getArea()).forEach(cell => {
      cell.unhighlight();
    });
  }

  /**
   * Searches a syntax node for gi
   * @param elementView
   * @private
   */
  private getIfLeafNode(elementView: dia.ElementView): PrimitiveDTO {
    if (NodeUtil.isSyntaxNode(elementView)) {
      const node = this.findSyntaxNodeById(elementView.model.id.toString());
      if (node['@class'] === 'PrimitiveDTO') {
        return node;
      }
    }
    return null;
  }

  /**
   * Finds syntax node for given id of semantic node
   * @param id of semantic node
   * @returns mapped syntax node
   * @private
   */
  private getMappedPrimitive(id: string): SyntaxNode {
    const syntaxNodeId = this.schema.primitiveToEntityTypeList.find(map => map.to === id).mappedPrimitive;
    return this.schema.syntaxModel.nodes.find(node => node.id === syntaxNodeId);
  }

  /**
   * Finds syntax node data model for given id
   * @param id
   * @private
   */
  private findSyntaxNodeById(id: string): SyntaxNode {
    return this.schema.syntaxModel.nodes.find(node => node.id === id);
  }

  /**
   * Finds semantic node data model for given id
   * @param id
   * @private
   */
  private findSemanticNodeById(id: string): EntityTypeDTO {
    return this.schema.semanticModel.nodes.find(node => node.id === id);
  }

  /**
   * Setter for entity concept
   * @param concept - the new concept
   */
  setEntityConcept(concept: EntityConceptDTO): void {
    this.entityConcept = concept;
  }

  /**
   * Transform primary and accent into hex values
   * @param color
   * @param contrast
   * @private
   */
  private colorToHex(color: 'primary' | 'accent', contrast: boolean = false): string {
    switch (color) {
      case 'primary':
        if (contrast) {
          return Util.rgbToHex(this.primaryColorContrast);
        } else {
          return Util.rgbToHex(this.primaryColor);
        }
      case 'accent':
        if (contrast) {
          return Util.rgbToHex(this.accentColorContrast);
        } else {
          return Util.rgbToHex(this.accentColor);
        }
    }
  }

  /**
   * Track mouse position during entity modeling to find target nodes
   * @param event
   */
  @HostListener('mousemove', ['$event'])
  onMousemove(event: MouseEvent): void {
    if (this.entityConcept) {
      this.mousePosition = {x: event.clientX, y: event.clientY};
    }
  }

  /**
   * Listen to window resize events
   */
  @HostListener('window:resize')
  onResize(): void {
    this.$resize.next(this.paper.getComputedSize());
  }


}
