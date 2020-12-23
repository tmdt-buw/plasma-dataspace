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
  ViewChild, ViewContainerRef
} from '@angular/core';
import { dia, g, layout, shapes } from 'jointjs';
import { BehaviorSubject, Subject } from 'rxjs';
import * as dagre from 'dagre';
import * as graphlib from 'graphlib';
import * as svgPanZoom from 'svg-pan-zoom';
import {
  CollisionSchemaDTO,
  CompositeDTO,
  DataSourceSchemaDTO,
  DeltaRecommendationDTO,
  EntityConceptDTO,
  EntityTypeDTO,
  ObjectNodeDTO,
  PrimitiveDTO,
  RelationConceptDTO,
  RelationDTO,
  SchemaNodeDTO,
  SemanticModelDTO,
  SetNodeDTO,
  SyntaxModelDTO
} from '../../api/generated/dms';
import { distinctUntilChanged, skip } from 'rxjs/operators';
import { Filter } from '../model/filter';
import { MatDialog, MatDialogConfig, MatDialogRef } from '@angular/material/dialog';
import { PlsSyntaxNodeDetailsComponent } from '../dialogs/syntax-node-dialog/syntax-node.component';
import { PlsSemanticNodeDetailsComponent } from '../dialogs/semantic-node-dialog/semantic-node.component';
import { EntityDroppedEvent } from '../model/events/entity-dropped-event';
import { NodeChangedEvent } from '../model/events/node-changed-event';
import { ModelingService } from '../modeling.service';
import { chain, each, filter, first, omit } from 'lodash';
import { Overlay } from '@angular/cdk/overlay';
import { ContextMenuService } from '../context-menu/context-menu.service';
import { PlsEntityConceptMenuComponent } from '../context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from '../context-menu/relation-concept-menu/relation-concept-menu.component';


@Component({
  selector: 'pls-modeling-diagram',
  templateUrl: './diagram.component.html',
  styleUrls: ['./diagram.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlsDiagramComponent implements OnInit, AfterViewInit {

  private readonly syntaxNode = 'custom.SyntaxNode';
  private readonly semanticNode = 'custom.SemanticNode';

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
  @Input() set schema(schema: DataSourceSchemaDTO) {
    this._schema = schema;
    if (!this.initializing) {
      this.createGraph();
    }
  }

  get schema(): DataSourceSchemaDTO {
    return this._schema;
  }

  @Input() initial: boolean;

  // graph elements
  @ViewChild('graph') canvas: ElementRef = null;
  private graph: dia.Graph;
  private paper: dia.Paper;
  private panAndZoom: SvgPanZoom.Instance;

  // dialog
  private nodeDialogRef: MatDialogRef<any>;

  // modeling
  entityConcept;
  relationConcept: RelationConceptDTO;
  hoveredElement;
  mousePosition;
  recommendations = [];

  @Output() graphLaidOut = new EventEmitter<DataSourceSchemaDTO>();
  @Output() entityConceptMapped = new EventEmitter<EntityDroppedEvent>();
  @Output() relationConceptLinked = new EventEmitter<RelationDTO>();
  @Output() nodeChanged = new EventEmitter<NodeChangedEvent>();
  @Output() nodePositionChanged = new EventEmitter<NodeChangedEvent>();

  constructor(public dialog: MatDialog, private overlay: Overlay, private modelingService: ModelingService, private contextMenu: ContextMenuService, private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    // init joint.js graph and paper
    this.graph = new dia.Graph({
      'add remove change:source change:target': (view) => {
        this.adjustVertices(this.graph, view);
      }
    });
    this.paper = new dia.Paper({
      // @ts-ignore
      model: this.graph,
      width: '100%',
      height: '100%',
      background: {
        color: '#fff',
      },
      gridSize: 1,
      frozen: true,
      async: true,
      linkPinning: false,
      interactive: () => {
        if (this.relationConcept) {
          return {
            labelMove: true,
            addLinkFromMagnet: true
          };
        } else {
          return {
            labelMove: true,
            addLinkFromMagnet: false
          };
        }
      },
      allowLink: (linkView, paper) => {
        return linkView.model.getTargetElement().attributes.type !== this.syntaxNode;
      },
      defaultLink: (cellView, magnet) => {
        const labels = [{
          attrs: {
            text: {
              text: this.relationConcept.name,
              fill: this.colorToHex('primary')
            }
          }
        }];
        return new shapes.standard.Link(
          {
            labels,
            attrs: {
              line: {
                stroke: this.colorToHex('primary'),
                strokeOpacity: 1,
                strokeWidth: 1
              }
            }
          }
        );
      }
    });
    // create nodes and links
    this.createGraph();
    // subscribe to relation tool
    this.modelingService.relationConcept.subscribe((relationSelection) => {
      this.relationConcept = relationSelection;
    });
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

  ngAfterViewInit(): void {
    // @ts-ignore
    this.canvas.nativeElement.append(this.paper.el);
    this.paper.unfreeze();
    // this.layout();
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
      'element:pointerclick': (elementView) => {
        this.unhighlightAll();
        elementView.highlight();
        const model = elementView.model.attributes;
        this.openDetails(model.id, model.type);
      },
      'element:pointerup': (elementView) => {
        const element = elementView.model;
        this.updatePosition(element, true);
      },
      // Highlights for modeling
      'element:mouseover': (elementView) => {
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
      'link:connect': (linkView) => {
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
        this.closeDialog();
      },
      // Enable pan when mouse button down in blank zone
      'blank:pointerdown': () => {
        this.panAndZoom.enablePan();
      },
      // Disable pan when the mouse button is released
      'cell:pointerup blank:pointerup': () => {
        this.panAndZoom.disablePan();
      },
      'cell:pointerup': (cell) => {
        this.adjustVertices(this.graph, cell);
      },
      'cell:contextmenu': (cellView) => {
        const model = cellView.model.attributes;
        if (model.linkType && model.linkType === 'semantic') {
          const link = this.schema.semanticModel.edges.find(edge => edge.id === model.id);
          this.contextMenu.open(cellView.el, PlsRelationConceptMenuComponent, this.viewContainerRef, {
            concept: link
          });
        }
        if (model.nodeType && model.nodeType === 'semantic') {
          const node = this.schema.semanticModel.nodes.find(node => node.id === model.id);
          this.unhighlightAll();
          cellView.highlight();
          this.contextMenu.open(cellView.el, PlsEntityConceptMenuComponent, this.viewContainerRef, {
              concept: node
            });
        }
      },
      // init layout after first rendering
      'render:done': () => {
        this.paper.off('render:done');
        if (this.initial) {
          // on first view layout and center
          this.layout();
          this.initial = false;
          setTimeout(() => this.center(), 100);
        } else {
          this.center();
        }
        this.initializing = false;
      }
    });
  }

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
        originalLabel: 'NOT_PART_OF_THE_SYNTAX_MODEL',
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

  private createGraph(flat: boolean = false): void {
    // remove old content
    this.paper.freeze();
    this.graph.clear();
    this.paper.updateViews();
    switch (this.$filter.getValue()) {
      case Filter.ALL:
        this.createSemanticGraph(this.schema.semanticModel, flat);
        const map = this.schema.primitiveToEntityTypeList;
        const mappedSyntaxNodes = Object.values(map).map(map => map.mappedPrimitive);
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
        this.createSyntaxGraph(this.schema.syntaxModel);
        break;
      case Filter.SEMANTIC:
        this.createSemanticGraph(this.schema.semanticModel, flat);
        break;
      default:
        console.error('Unsupported Filter');
    }
    this.paper.unfreeze();
    this.graph.getLinks().forEach(el => this.adjustVertices(this.graph, el));
  }

  private createSemanticGraph(semanticModel: SemanticModelDTO, flat: boolean) {
    if (semanticModel.nodes.length > 0) {
      semanticModel.nodes.forEach(node => {
        let icon = '';
        if (node.mappedToData) {
          const syntaxNode = this.getMappedPrimitve(node.id);
          icon = this.getIcon(syntaxNode);
        }
        this.addSemanticNode(node.id, flat ? node.concept.sourceURI : node.concept.name, node.label, icon, 'primary', node.x, node.y);
      });
      semanticModel.edges.forEach(edge => this.addLink(edge.id, edge.from, edge.to, false, flat ? edge.concept.sourceURI : edge.concept.name));
    }
  }

  private getMappedPrimitve(id: string) {
    const syntaxNodeId = this.schema.primitiveToEntityTypeList.find(map => map.to === id).mappedPrimitive;
    return this.schema.syntaxModel.nodes.find(node => node.id === syntaxNodeId);
  }

  private createSyntaxGraph(syntaxModel: SyntaxModelDTO) {
    if (syntaxModel.nodes.length > 0) {
      syntaxModel.nodes.forEach(node => this.addSyntaxNode(node.id, node.label, this.getIcon(node), node.x, node.y));
      syntaxModel.edges.forEach(edge => this.addLink(null, edge.from, edge.to, true));
    }
  }

  private showRecommendation(recommendation: DeltaRecommendationDTO): void {
    recommendation.entities.forEach(node => {
      let icon = '';
      if (node.mappedToData) {
        const syntaxNode = this.getMappedPrimitve(node.id);
        icon = this.getIcon(syntaxNode);
      }
      this.recommendations.push(this.addSemanticNode(node.id, node.concept.name, node.label, icon, 'accent', node.x, node.y, 0.6));
    });
    recommendation.relations.forEach(edge => this.recommendations.push(this.addLink(edge.id, edge.from, edge.to, false, edge.concept.name, 'accent')));
  }

  private hideRecommendation(): void {
    this.graph.removeCells(this.recommendations);
    this.recommendations = [];
  }

  private addSemanticNode(id: string, concept: string, label: string, icon: string, color: 'primary' | 'accent' = 'primary', x?: number, y?: number, opacity?: number): dia.Element {
    const conceptDims = this.calcDimensionsForLabel(concept, false);
    const labelDims = this.calcDimensionsForLabel(label, !!icon);
    const dim = [Math.max(conceptDims[0], labelDims[0]), conceptDims[1] + labelDims[1]];
    const position = {x, y};
    const node = new this.addCustomSemanticNode({
      id,
      // @ts-ignore
      position,
      // @ts-ignore
      size: {
        width: dim[0],
        height: dim[1]
      },
      nodeType: 'semantic',
      attrs: {
        body: {
          rx: 5,
          ry: 5,
          strokeWidth: 2,
          stroke: this.colorToHex(color),
          opacity: opacity ? opacity : 1,
          magnet: true
        },
        header: {
          rx: 5,
          ry: 5,
          fill: this.colorToHex(color),
          stroke: this.colorToHex(color),
          opacity: opacity ? opacity : 1
        },
        headerText: {
          text: concept,
          fill: this.colorToHex(color, true),
          cursor: 'default',
        },
        bodyIcon: {
          text: icon,
          cursor: 'default',
          fill: this.colorToHex(color),
        },
        bodyText: {
          // adjust text for icon
          refX: icon ? '35' : '50%',
          textAnchor: icon ? 'left' : 'middle',
          text: label,
          cursor: 'default',
          fill: this.colorToHex(color),
        }
      }
    });
    this.graph.addCell(node);
    return node;
  }

  private addSyntaxNode(id: string, label: string, icon: string, x?: number, y?: number): void {
    const dim = this.calcDimensionsForLabel(label, true);
    const position = {x, y};
    const node = new this.addCustomSyntaxNode({
      id,
      // @ts-ignore
      position,
      // @ts-ignore
      size: {
        width: dim[0],
        height: dim[1]
      },
      attrs: {
        body: {
          rx: 5,
          ry: 5,
          strokeWidth: 2,
          stroke: this.colorToHex('primary'),
          strokeOpacity: 0.75,
          strokeDasharray: '10,2'
        },
        icon: {
          text: icon,
          fill: this.colorToHex('primary'),
          cursor: 'default'
        },
        label: {
          text: label,
          fill: this.colorToHex('primary'),
          cursor: 'default'
        }
      }
    });
    this.graph.addCell(node);
  }

  private calcDimensionsForLabel(label: string, icon: boolean): [number, number] {
    const letterSize = 7;
    const iconWidth = icon ? 30 : 0;
    const width = this.getTextWidth(label, '11pt Roboto') + 10 + iconWidth;
    const height = 2 * ((label.split('\n').length + 1) * letterSize);
    return [width, height];
  }

  private _canvas = document.createElement('canvas');

  private getTextWidth(text, font) {
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

  private layout(): void {
    layout.DirectedGraph.layout(this.graph, {
      dagre,
      graphlib,
      nodeSep: 60,
      edgeSep: 80,
      rankDir: 'TB'
    });
    setTimeout(() => {
      this.graph.getElements().forEach((element) => {
        this.updatePosition(element, false);
      });
      setTimeout(() => this.graph.getLinks().forEach(el => this.adjustVertices(this.graph, el), 300));
      this.graphLaidOut.emit(this.schema);
    }, 500);
  }

  private updatePosition(element: dia.Element, emitEvent: boolean): void {
    const model = element.attributes;
    if (model.type === this.syntaxNode) {
      const node = this.schema.syntaxModel.nodes.find(node => node.id === model.id);
      const position = model.position;
      if (node && (node.x !== position.x || node.y !== position.y)) {
        node.x = position.x;
        node.y = position.y;
        if (emitEvent) {
          const nodeChanges = new NodeChangedEvent('syntax', node);
          this.nodePositionChanged.emit(nodeChanges);
        }
      }
    }
    if (model.type === this.semanticNode) {
      const node = this.schema.semanticModel.nodes.find(node => node.id === model.id);
      const position = model.position;
      if (node && (node.x !== position.x || node.y !== position.y)) {
        node.x = position.x;
        node.y = position.y;
        if (emitEvent) {
          const nodeChanges = new NodeChangedEvent('semantic', node);
          this.nodePositionChanged.emit(nodeChanges);
        }
        if (node.mappedToData) {
          const syntaxNode = this.getMappedPrimitve(node.id);
          syntaxNode.x = position.x;
          syntaxNode.y = position.y;
          if (emitEvent) {
            const nodeChanges = new NodeChangedEvent('syntax', syntaxNode);
            this.nodePositionChanged.emit(nodeChanges);
          }
        }
      }
    }
  }

  private getIcon(node: SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO): string {
    switch (node['@class']) {
      case 'ObjectNodeDTO':
        return 'list';
      case 'PrimitiveDTO':
        const value = ((node as PrimitiveDTO).dataType);
        switch (value) {
          case 'Number':
            return 'looks_one';
          case 'String':
            return 'text_fields';
          case 'Boolean':
            return 'check_box';
          case 'Binary':
            return 'attach_file';
          case 'Unknown':
            return 'not_listed_location';
          default:
            return '';
        }
      case 'CompositeDTO':
        return 'content_cut';
      case 'SetNodeDTO':
        return 'format_list_numbered';
      default:
        return '';
    }
  }

  private addCustomSyntaxNode = shapes.standard.Rectangle.define(
    this.syntaxNode, {
      attrs: {
        icon: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '5',
          refY: '65%',
          fontSize: 24,
          fill: '#333333',
          'font-family': 'Material Icons'
        },
        label: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '35',
          refY: '50%',
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto'
        }
      }
    }, {
      markup: [{
        tagName: 'rect',
        selector: 'body',
      }, {
        tagName: 'text',
        selector: 'icon'
      }, {
        tagName: 'text',
        selector: 'label'
      }]
    });


  private addCustomSemanticNode = shapes.standard.HeaderedRectangle.define(
    this.semanticNode, {
      attrs: {
        headerText: {
          textVerticalAnchor: 'middle',
          textAnchor: 'middle',
          refX: '50%',
          refY: 15,
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto'
        },
        bodyIcon: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '5',
          refY: '58%',
          refY2: 15,
          fontSize: 24,
          fill: '#333333',
          'font-family': 'Material Icons'
        },
        bodyText: {
          textVerticalAnchor: 'middle',
          textAnchor: 'left',
          refX: '35',
          refY: '50%',
          refY2: 15,
          fontSize: 14,
          fill: '#333333',
          'font-family': 'Roboto'
        }
      }
    }, {
      markup: [{
        tagName: 'rect',
        selector: 'body'
      }, {
        tagName: 'rect',
        selector: 'header'
      }, {
        tagName: 'text',
        selector: 'headerText'
      }, {
        tagName: 'text',
        selector: 'bodyIcon'
      }, {
        tagName: 'text',
        selector: 'bodyText'
      }]
    });

  private openDetails(id: string, type: string): void {
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
      case this.syntaxNode:
        config.data.node = this.schema.syntaxModel.nodes.find(node => node.id === id);
        this.nodeDialogRef = this.dialog.open(PlsSyntaxNodeDetailsComponent, config);
        this.nodeDialogRef.beforeClosed().subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
          }
        });
        break;
      case this.semanticNode:
        config.data.node = this.schema.semanticModel.nodes.find(node => node.id === id);
        this.nodeDialogRef = this.dialog.open(PlsSemanticNodeDetailsComponent, config);
        this.nodeDialogRef.beforeClosed().subscribe((res) => {
          if (res) {
            this.nodeDialogRef = null;
          }
          const el = this.graph.getElements().find(el => el.attributes.id === id);
          // TODO update width
          el.attr('bodyText/text', config.data.node.label);
        });
        break;
      default:
        console.error('Unsupported node type');
    }
  }

  private closeDialog(): void {
    if (this.nodeDialogRef) {
      this.nodeDialogRef.close();
      this.nodeDialogRef = null;
    }
  }

  private unhighlightAll(): void {
    this.closeDialog();
    this.paper.findViewsInArea(this.paper.getArea()).forEach(cell => {
      cell.unhighlight();
    });
  }

  private getIfLeafNode(cellView): PrimitiveDTO {
    if (cellView.model?.attributes?.type === this.syntaxNode) {
      const node = this.schema.syntaxModel.nodes.find(node => node.id === cellView.model.id);
      if (node['@class'] === 'PrimitiveDTO') {
        return node;
      }
    }
    return null;
  }

  setEntityConcept(concept: EntityConceptDTO) {
    this.entityConcept = concept;
  }

  @HostListener('mousemove', ['$event'])
  onMousemove(event: MouseEvent) {
    if (this.entityConcept) {
      this.mousePosition = {x: event.clientX, y: event.clientY};
    }
  }

  private colorToHex(color: 'primary' | 'accent', contrast: boolean = false): string {
    switch (color) {
      case 'primary':
        if (contrast) {
          return this.rgbToHex(this.primaryColorContrast);
        } else {
          return this.rgbToHex(this.primaryColor);
        }
      case 'accent':
        if (contrast) {
          return this.rgbToHex(this.accentColorContrast);
        } else {
          return this.rgbToHex(this.accentColor);
        }
    }
  }

  private rgbToHex(rgbString: string): string {
    const rgb = rgbString.split(',');
    const r = parseInt(rgb[0].substring(4));
    const g = parseInt(rgb[1]);
    const b = parseInt(rgb[2]);
    return '#' + [r, g, b].map(x => {
      const hex = x.toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    }).join('');
  }

  @HostListener('window:resize', ['$event'])
  onResize(event): void {
    console.log(event);
    // const width = +getComputedStyle(this.canvas.nativeElement).width.replace('px', '');
    // const height = +getComputedStyle(this.canvas.nativeElement).height.replace('px', '');
    // const zoom = this.panAndZoom.getZoom();
    // console.log(width, height);
    // this.graph.resize(width / zoom, height / zoom);
  }


  private adjustVertices(graph, cell): void {

    // if `cell` is a view, find its model
    cell = cell.model || cell;

    if (cell instanceof dia.Element) {
      // `cell` is an element

      chain(graph.getConnectedLinks(cell))
        .groupBy((link) => {

          // the key of the group is the model id of the link's source or target
          // cell id is omitted
          return omit([link.source().id, link.target().id], cell.id)[0];
        })
        .each((group, key) => {

          // if the member of the group has both source and target model
          // then adjust vertices
          if (key !== 'undefined') {
            this.adjustVertices(graph, first(group));
          }
        })
        .value();

      return;
    }

    // `cell` is a link
    // get its source and target model IDs
    const sourceId = cell.get('source').id || cell.previous('source').id;
    const targetId = cell.get('target').id || cell.previous('target').id;

    // if one of the ends is not a model
    // (if the link is pinned to paper at a point)
    // the link is interpreted as having no siblings
    if (!sourceId || !targetId) {
      return;
    }

    // identify link siblings
    const siblings = filter(graph.getLinks(), (sibling) => {

      const siblingSourceId = sibling.source().id;
      const siblingTargetId = sibling.target().id;

      // if source and target are the same
      // or if source and target are reversed
      return ((siblingSourceId === sourceId) && (siblingTargetId === targetId))
        || ((siblingSourceId === targetId) && (siblingTargetId === sourceId));
    });

    const numSiblings = siblings.length;
    switch (numSiblings) {

      case 0: {
        // the link has no siblings
        break;

      }
      case 1: {
        // there is only one link
        // no vertices needed
        cell.unset('vertices');
        break;

      }
      default: {
        // there are multiple siblings
        // we need to create vertices

        // find the middle point of the link
        const sourceCenter = graph.getCell(sourceId).getBBox().center();
        const targetCenter = graph.getCell(targetId).getBBox().center();
        const midPoint = new g.Line(sourceCenter, targetCenter).midpoint();

        // find the angle of the link
        const theta = sourceCenter.theta(targetCenter);

        // constant
        // the maximum distance between two sibling links
        const GAP = 50;

        each(siblings, (sibling, index) => {

          // we want offset values to be calculated as 0, 20, 20, 40, 40, 60, 60 ...
          let offset = GAP * Math.ceil(index / 2);

          // place the vertices at points which are `offset` pixels perpendicularly away
          // from the first link
          //
          // as index goes up, alternate left and right
          //
          //  ^  odd indices
          //  |
          //  |---->  index 0 sibling - centerline (between source and target centers)
          //  |
          //  v  even indices
          const sign = ((index % 2) ? 1 : -1);

          // to assure symmetry, if there is an even number of siblings
          // shift all vertices leftward perpendicularly away from the centerline
          if ((numSiblings % 2) === 0) {
            offset -= ((GAP / 2) * sign);
          }

          // make reverse links count the same as non-reverse
          const reverse = ((theta < 180) ? 1 : -1);

          // we found the vertex
          const angle = g.toRad(theta + (sign * reverse * 90));
          const vertex = g.Point.fromPolar(offset, angle, midPoint);

          // replace vertices array with `vertex`
          sibling.vertices([vertex]);
        });
      }
    }
  }
}
