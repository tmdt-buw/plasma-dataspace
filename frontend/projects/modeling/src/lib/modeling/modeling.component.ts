import { animate, style, transition, trigger } from '@angular/animations';
import { Component, ElementRef, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CombinedModel, DataModelingControllerService, EntityType, Relation } from '../api/generated/dms';
import { LocalKnowledgeControllerService, UniversalKnowledgeGraphControllerService } from '../api/generated/kgs';
import { ContextMenuService } from './context-menu/context-menu.service';
import { PlsConceptDialogComponent } from './dialogs/concept-dialog/concept-dialog.component';
import { PlsImportDialogComponent } from './dialogs/import-dialog/import-dialog.component';
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
  styleUrls: ['./modeling.component.scss'],
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
export class PlsModelingComponent implements OnInit {

  readonly Filter = Filter;

  @Input() dataSource: string;
  @Input() backgroundColor: string;
  @Input() fullScreen: boolean = true;

  @ViewChild('main') main;
  @ViewChild('primary') primary;
  @ViewChild('accent') accent;
  @ViewChild('modelingArea') modelingArea: ElementRef;

  // search bar
  searchControl = new FormControl();
  isSearchExpanded = false;

  // interactions
  $center = new Subject<any>();
  $layout = new Subject<any>();
  $filter = new BehaviorSubject<Filter>(Filter.ALL);
  $flat = new BehaviorSubject<boolean>(false);

  // colors
  primaryColor;
  primaryColorContrast;
  accentColor;
  accentColorContrast;

  // schema
  schema: CombinedModel;
  initial: boolean = false;
  loading: boolean = true;

  // dialogs
  addConceptDialogRef;

  get isFullscreen(): boolean {
    return !!this.main?.fullScreenIsActive;
  }

  hasColors(): boolean {
    return this.primaryColor && this.primaryColorContrast && this.accentColor && this.accentColorContrast;
  }

  constructor(private modelingService: DataModelingControllerService, private contextMenu: ContextMenuService, public dialog: MatDialog,
              private kgsService: UniversalKnowledgeGraphControllerService, private kgsModelService: LocalKnowledgeControllerService, private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    // query schema
    this.getSchema();
    // get angular material primary color
    setTimeout(() => {
      this.primaryColor = getComputedStyle(this.primary._elementRef.nativeElement).backgroundColor;
      this.primaryColorContrast = getComputedStyle(this.primary._elementRef.nativeElement).color;
      this.accentColor = getComputedStyle(this.accent._elementRef.nativeElement).backgroundColor;
      this.accentColorContrast = getComputedStyle(this.accent._elementRef.nativeElement).color;
    });
    // subscribe to search
    this.searchControl.valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(1000)
    ).subscribe((value) => {
      // TODO
    });
    this.contextMenu.onClosed.subscribe((event: ContextMenuEvent) => {
      if (event) {
        if (event.type === ContextMenuEventType.removeEntity) {
          this.modelingService.removeEntityType(this.dataSource, (event.target as EntityType).uuid).subscribe((res: CombinedModel) => this.schema = res);
        }
        if (event.type === ContextMenuEventType.removeRelation) {
          this.modelingService.removeRelation(this.dataSource, (event.target as Relation).uuid).subscribe((res: CombinedModel) => this.schema = res);
        }
        if (event.type === ContextMenuEventType.performOperation) {
          const dialogRef = this.dialog.open(PlsOperationsDialogComponent, {
            data: {
              operation: event.operation
            }
          });
          dialogRef.afterClosed().subscribe(context => {
            if (context) {
              this.modelingService.modifySchema(this.dataSource, context.operation).subscribe((res: CombinedModel) => this.schema = res);
            }
          });
        }
      }
    });
  }

  getSchema(): void {
    this.modelingService.getCombinedModel(this.dataSource).subscribe((schema: CombinedModel) => {
        this.schema = schema;
        this.loading = false;
      },
      () => this.modelingService.initDataSourceSchema(this.dataSource).subscribe(() => {
        this.initial = true;
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
    const parentHeight = getComputedStyle(this.main._el.nativeElement).height.replace('px', '');
    if (this.addConceptDialogRef) {
      this.addConceptDialogRef.close();
      return;
    }
    this.addConceptDialogRef = this.dialog.open(PlsConceptDialogComponent, {
      autoFocus: false,
      hasBackdrop: false,
      data: {
        dataSource: this.dataSource,
        node: null,
        relativeTo: this.modelingArea
      },
      maxHeight: `${(+parentHeight - 70)}px`,
      maxWidth: '440px',
      closeOnNavigation: true,
      viewContainerRef: this.viewContainerRef
    });
    this.addConceptDialogRef.afterClosed().subscribe(() => this.addConceptDialogRef = undefined);
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

  onFilterChange(event: Filter): void {
    this.$filter.next(event);
  }

  onImport(): void {
    this.dialog.open(PlsImportDialogComponent);
  }

  onExportKnowledgeGraph(): void {
    this.kgsService.convertGraphToRdf('turtle', true).subscribe((res) => Util.download('ontology.rdf', res));
  }

  onExportOntology(): void {
    this.kgsService.getUpperOntology('turtle').subscribe((res) => Util.download('upperOntology.rdf', res));
  }

  finalizeModel(): void {
    this.modelingService.finishModeling(this.dataSource).subscribe((res) => this.schema = res);
  }

  exportModel(): void {
    this.kgsModelService.convertSemanticModel(this.schema.semanticModel.entityId, 'turtle', this.$flat.getValue()).subscribe((res) => Util.download('model.rdf', res));
  }
}
