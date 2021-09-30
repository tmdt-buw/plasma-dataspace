import { animate, style, transition, trigger } from '@angular/animations';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ModalOptions, NzModalService } from 'ng-zorro-antd/modal';
import { BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  CombinedModel,
  DataModelingControllerService,
  DeltaModification,
  EntityType,
  Relation
} from '../api/generated/dms';
import { LocalKnowledgeControllerService, UniversalKnowledgeGraphControllerService } from '../api/generated/kgs';
import { ContextMenuService } from './context-menu/context-menu.service';
import { ModalBaseConfig, ModalMouseEnabledConfig } from './dialogs/common/modal.base-config';
import { PlsConceptDialogComponent } from './dialogs/concept-dialog/concept-dialog.component';
import { PlsOperationsDialogComponent } from './dialogs/operations-dialog/operations-dialog/operations-dialog.component';
import { Filter } from './model/common/filter';
import { Util } from './model/common/util';
import { Nodes, SyntaxNode } from './model/configuration/node';
import { ContextMenuEvent, ContextMenuEventType } from './model/events/context-menu-event';
import { EntityDroppedEvent } from './model/events/entity-dropped-event';
import { NodeChangedEvent } from './model/events/node-changed-event';

@Component({
  selector: 'pls-modeling',
  templateUrl: './modeling.component.html',
  styleUrls: ['./modeling.component.less'],
  animations: [
    // search bar animation
    trigger('search', [
      transition(':enter', [
        style({width: '0'}),
        animate('300ms', style({width: '180px'}))
      ]),
      transition(':leave', [
        animate('300ms', style({width: '0'}))
      ])
    ])
  ]
})
export class PlsModelingComponent implements OnInit, OnDestroy {

  readonly Filter = Filter;

  @Input() dataSource: string;
  @Input() backgroundColor: string;
  @Input() fullScreen: boolean = true;
  @Input() readonly: boolean = false;
  @Input() lockDragAndDrop: boolean = false;
  @Input() hideFinalAndExportButton: boolean = false;

  @ViewChild('main') main;
  @ViewChild('modelingArea') modelingArea: ElementRef;

  @Output() finished = new EventEmitter();
  @Output() clickedNode = new EventEmitter();

  // search bar
  searchControl = new FormControl();
  isSearchExpanded = false;

  // interactions
  $center = new Subject<any>();
  $layout = new Subject<any>();
  $filter = new BehaviorSubject<Filter>(Filter.ALL);
  $flat = new BehaviorSubject<boolean>(false);

  // schema
  schema: CombinedModel;
  loading: boolean = true;

  // dialogs
  addConceptDialogRef;
  contextMenuSubscription;

  get isFullscreen(): boolean {
    return !!this.main?.fullScreenIsActive;
  }

  constructor(private modelingService: DataModelingControllerService, private contextMenu: ContextMenuService, private kgsService: UniversalKnowledgeGraphControllerService,
              private kgsModelService: LocalKnowledgeControllerService, private modal: NzModalService) {
  }

  ngOnInit(): void {
    // query schema
    this.getSchema();
    // subscribe to search
    this.searchControl.valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(1000)
    ).subscribe((value) => {
      // TODO
    });
    this.contextMenuSubscription = this.contextMenu.onClosed.subscribe((event: ContextMenuEvent) => {
      if (event) {
        if (event.type === ContextMenuEventType.removeEntity) {
          this.modelingService.removeEntityType(this.dataSource, (event.target as EntityType).uuid).subscribe((res: CombinedModel) => this.schema = res);
        }
        if (event.type === ContextMenuEventType.removeRelation) {
          this.modelingService.removeRelation(this.dataSource, (event.target as Relation).uuid).subscribe((res: CombinedModel) => this.schema = res);
        }
        if (event.type === ContextMenuEventType.performOperation) {
          const operation = event.operation;
          const config: ModalOptions = {
            nzTitle: operation.label,
            nzContent: PlsOperationsDialogComponent,
            nzComponentParams: {
              operation
            }
          };
          const dialogRef = this.modal.create(Object.assign(config, ModalBaseConfig));
          dialogRef.afterClose.subscribe(context => {
            if (context) {
              this.modelingService.modifySchema(this.dataSource, context).subscribe((res: CombinedModel) => this.schema = res);
            }
          });
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.modal.closeAll();
    this.contextMenuSubscription.unsubscribe();
    this.addConceptDialogRef?.close();
  }

  getSchema(): void {
    this.modelingService.getCombinedModel(this.dataSource).subscribe((schema: CombinedModel) => {
        this.schema = schema;
        if (this.readonly) {
          this.$filter.next(Filter.SEMANTIC);
        }
        this.loading = false;
      },
      () => this.modelingService.initDataSourceSchema(this.dataSource).subscribe(() => {
        this.loading = false;
        this.getSchema();
      }));
  }

  // ======= Menu functions ===========

  undo(): void {
    this.modelingService.undoModification(this.dataSource).subscribe((res: CombinedModel) => this.schema = res);
  }

  redo(): void {
    this.modelingService.redoModification(this.dataSource).subscribe((res: CombinedModel) => this.schema = res);
  }

  addConcepts(): void {
    if (this.addConceptDialogRef) {
      this.addConceptDialogRef.close();
    }
    const rect: DOMRect = this.modelingArea.nativeElement.getBoundingClientRect();
    const config: ModalOptions = {
      nzStyle: {position: 'absolute', top: `${rect.top + 15}px`, left: `15px`, 'max-width': '30%', bottom: '15px'},
      nzClassName: 'pls-concept-dialog',
      nzTitle: '',
      nzContent: PlsConceptDialogComponent,
      nzComponentParams: {
        dataSource: this.dataSource
      }
    };
    this.addConceptDialogRef = this.modal.create(Object.assign(config, ModalMouseEnabledConfig));
  }

  onNodeClicked(event: string): void {
    this.clickedNode.emit(event);
  }

  onRelationConceptLinked(event: Relation): void {
    this.modelingService.addRelation(this.dataSource, event).subscribe((res) => this.schema = res);
  }

  onConceptMapped(event: EntityDroppedEvent): void {
    this.modelingService.addEntityType(this.dataSource, event.entityType, event.primitiveNodeUUID).subscribe((res) => this.schema = res);
  }

  onGraphLaidOut(event: CombinedModel): void {
    this.modelingService.updatePositions(this.dataSource, event.semanticModel.nodes.concat(event.syntaxModel.nodes)).subscribe();
  }

  onNodePositionChanged(event: NodeChangedEvent): void {
    switch (event.type) {
      case Nodes.syntaxNodeName:
        this.modelingService.positionSchemaNode(this.dataSource, event.node.uuid, event.node as SyntaxNode).subscribe();
        break;
      case Nodes.semanticNodeName:
        this.modelingService.positionEntityType(this.dataSource, event.node.uuid, event.node as EntityType).subscribe();
        break;
      default:
        console.error('Unsupported type.');
    }
  }

  onNodeChanged(event: NodeChangedEvent): void {
    switch (event.type) {
      case Nodes.semanticNodeName:
        this.modelingService.updateEntityType(this.dataSource, event.node.uuid, event.node as EntityType).subscribe();
        break;
      default:
        console.error('Unsupported type.');
    }
  }

  toggleFlatMode(event): void {
    this.$flat.next(event.checked);
  }

  /**
   * toggle search bar visibility
   */
  toggleSearch(): void {
    this.isSearchExpanded = !this.isSearchExpanded;
  }

  onFilterChange(event): void {
    this.$filter.next(event);
  }

  onRecommendationAccepted(event: DeltaModification): void {
    console.log('accepting recommendation', event);
    this.modelingService.acceptRecommendation(this.dataSource, event).subscribe(cm =>
      this.schema = cm
    );
  }

  finalizeModel(): void {
    // close all dialogs
    this.modal.closeAll();
    this.readonly = true;
    this.modelingService.finishModeling(this.dataSource).subscribe(() => {
      this.finished.emit();
    }, () => {
      this.readonly = false;
    });
  }

  exportModel(): void {
    this.kgsModelService.convertSemanticModel(this.schema.semanticModel.entityId, 'turtle', this.$flat.getValue()).subscribe((res) => Util.download('model.rdf', res));
  }
}
