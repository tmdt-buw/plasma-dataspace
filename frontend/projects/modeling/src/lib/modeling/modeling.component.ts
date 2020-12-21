import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { animate, style, transition, trigger } from '@angular/animations';
import { BehaviorSubject, Subject } from 'rxjs';
import {
  CollisionSchemaDTO,
  CompositeDTO,
  DataModelingControllerService,
  DataSourceSchemaDTO,
  ObjectNodeDTO,
  PrimitiveDTO,
  RelationDTO,
  SchemaNodeDTO,
  SetNodeDTO
} from '../api/generated/dms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Filter } from './model/filter';
import { MatDialog } from '@angular/material/dialog';
import { PlsConceptDialogComponent } from './dialogs/concept-dialog/concept-dialog.component';
import { EntityDroppedEvent } from './model/entity-dropped-event';
import { NodeChangedEvent } from './model/node-changed-event';
import { PlsImportDialogComponent } from './dialogs/import-dialog/import-dialog.component';
import { UniversalKnowledgeGraphControllerService } from '../api/generated/dms/api/kgsController.service';
import { LocalKnowledgeControllerService } from '../api/generated/dms/api/kgsModelController.service';
import { ContextMenuService } from './context-menu/context-menu.service';
import { ContextMenuEvent, ContextMenuEventType } from './context-menu/context-menu-event';

@Component({
  selector: 'pls-modeling',
  templateUrl: './modeling.component.html',
  styleUrls: ['./modeling.component.scss'],
  animations: [
    // search bar animation
    trigger('search', [
      transition(':enter', [
        style({width: '0'}),
        animate('300ms', style({width: '180px'})),
      ]),
      transition(':leave', [
        animate('300ms', style({width: '0'}))
      ])
    ]),
  ]
})
export class PlsModelingComponent implements OnInit, OnDestroy {

  readonly Filter = Filter;

  @Input() dataSource: string;

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

  //colors
  primaryColor;
  primaryColorContrast;
  accentColor;
  accentColorContrast;

  //schema
  schema: DataSourceSchemaDTO; // = test2;
  initial: boolean = false;
  loading: boolean = true;

  get isFullscreen(): boolean {
    return !!document.fullscreenElement;
  }

  hasColors(): boolean {
    return this.primaryColor && this.primaryColorContrast && this.accentColor && this.accentColorContrast;
  }

  constructor(private modelingService: DataModelingControllerService, private contextMenu: ContextMenuService, public dialog: MatDialog, private kgsService: UniversalKnowledgeGraphControllerService, private kgsModelService: LocalKnowledgeControllerService) {
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
          this.modelingService.removeEntityType(this.dataSource, +event.concept.id).subscribe((res) => this.schema = res);
        }
        if (event.type === ContextMenuEventType.removeRelation) {
          this.modelingService.removeRelation(this.dataSource, +event.concept.id).subscribe((res) => this.schema = res);
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (document.fullscreenElement) {
      document.exitFullscreen();
    }
  }

  getSchema(): void {
    this.modelingService.getDataSourceSchema(this.dataSource).subscribe((schema) => {
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
    this.modelingService.undoDataSourceSchemaModification(this.dataSource).subscribe((res: DataSourceSchemaDTO) => this.schema = res);
  }

  redo(): void {
    this.modelingService.redoDataSourceSchemaModification(this.dataSource).subscribe((res: DataSourceSchemaDTO) => this.schema = res);
  }

  addConcepts(): void {
    const parentHeight = getComputedStyle(this.main.nativeElement).height.replace('px', '');
    this.dialog.open(PlsConceptDialogComponent, {
      hasBackdrop: false,
      data: {
        dataSource: this.dataSource,
        node: null,
        relativeTo: this.modelingArea
      },
      maxHeight: `${(+parentHeight - 70)}px`,
      maxWidth: '440px',
      closeOnNavigation: true
    });
  }

  onRelationConceptLinked(event: RelationDTO): void {
    this.modelingService.addRelation(this.dataSource, event).subscribe((res) => this.schema = res);
  }

  onConceptMapped(event: EntityDroppedEvent): void {
    this.modelingService.addEntityType(this.dataSource, event.entityConceptDTO, event.primitiveNodeUUID).subscribe((res) => this.schema = res);
  }

  onGraphLaidOut(event: DataSourceSchemaDTO): void {
    this.modelingService.updatePositions(this.dataSource, event).subscribe();
  }

  onNodePositionChanged(event: NodeChangedEvent): void {
    switch (event.type) {
      case 'syntax':
        this.modelingService.positionSchemaNode(this.dataSource, event.node.id, event.node as SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO).subscribe();
        break;
      case 'semantic':
        this.modelingService.positionEntityType(this.dataSource, +event.node.id, event.node).subscribe();
        break;
      default:
        console.error('Unsupported type.');
    }
  }

  onNodeChanged(event: NodeChangedEvent): void {
    switch (event.type) {
      case 'syntax':
        this.modelingService.updateEntityType(this.dataSource, +event.node.id, event.node as SchemaNodeDTO | CollisionSchemaDTO | CompositeDTO | ObjectNodeDTO | PrimitiveDTO | SetNodeDTO).subscribe();
        break;
      default:
        console.error('Unsupported type.');
    }
  }

  toggleFlatMode(event) {
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

  onExportKnowledgeGraph() {
    this.kgsService.convertGraphToRdf('turtle', true).subscribe((res) => this.download('ontology.rdf', res));
  }

  onExportOntology() {
    this.kgsService.getUpperOntology('turtle').subscribe((res) => this.download('upperOntology.rdf', res));
  }

  toggleFullScreen(): void {
    if (document) {
      if (document.fullscreenElement) {
        document.exitFullscreen();
      } else {
        document.documentElement.requestFullscreen();
      }
    }
  }

  private download(filename: string, content: string) {
    const blob = new Blob([content], {type: 'application/xml'});
    const link = document.createElement('a');
    link.setAttribute('type', 'hidden');
    link.href = window.URL.createObjectURL(blob);
    link.download = filename;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  finalizeModel() {
    this.modelingService.finishModeling(this.dataSource).subscribe((res) => this.schema = res);
  }

  exportModel() {
    this.kgsModelService.convertSemanticModel(this.schema.semanticModel.uuid, 'turtle', this.$flat.getValue()).subscribe((res) => this.download('model.rdf', res));
  }
}
