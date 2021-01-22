import {AfterViewInit, Component, ElementRef, Inject, OnInit, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {
  DataModelingControllerService,
  DeltaModification,
  EntityConcept,
  RelationConcept
} from '../../../api/generated/dms';
import {FormControl} from '@angular/forms';
import {debounceTime, distinctUntilChanged} from 'rxjs/operators';
import {PlsNewEntityDialogComponent} from '../new-entity-dialog/new-entity-dialog.component';
import {PlsNewRelationDialogComponent} from '../new-relation-dialog/new-relation-dialog.component';
import {ModelingService} from '../../modeling.service';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {MatSelectionList} from '@angular/material/list';

export interface PlsConceptDialogData {
  dataSource: string;
  node: any;
  relativeTo: ElementRef;
  recommendations: DeltaModification[];
}


@Component({
  selector: 'pls-concept-dialog',
  templateUrl: 'concept-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'concept-dialog.component.scss']
})
export class PlsConceptDialogComponent implements OnInit, AfterViewInit {

  @ViewChild('colorEl') color;
  @ViewChild('relationsList') relationsList: MatSelectionList;

  private relativeTo: ElementRef;

  entitySearchControl = new FormControl('');
  relationSearchControl = new FormControl('');

  loadingEntityConcepts = true;
  loadingRelationConcepts = true;

  entityConcepts: EntityConcept[] = [];
  relationConcepts: RelationConcept[] = [];

  primaryColor;
  primaryColorContrast;

  // TODO find useful recommendation view
  constructor(public dialogRef: MatDialogRef<PlsConceptDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: PlsConceptDialogData,
              private modeling: ModelingService, private modelingService: DataModelingControllerService, public dialog: MatDialog) {
    this.relativeTo = data.relativeTo;
  }

  ngOnInit(): void {
    const rect: DOMRect = this.relativeTo.nativeElement.getBoundingClientRect();
    const position = {top: `${rect.top + 15}px`, left: `${rect.left + 15}px`};
    this.dialogRef.updatePosition(position);
    this.entitySearchControl.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => this.getEntityConcepts());
    this.relationSearchControl.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => this.getRelationConcepts());
    this.getEntityConcepts();
    this.getRelationConcepts();
  }

  ngAfterViewInit(): void {
    this.primaryColor = getComputedStyle(this.color._elementRef.nativeElement).background;
    this.primaryColorContrast = getComputedStyle(this.color._elementRef.nativeElement).color;
  }

  getEntityConcepts(): void {
    this.loadingEntityConcepts = true;
    this.modelingService.getEntityConcepts(this.data.dataSource, this.entitySearchControl.value).subscribe((res) => {
      this.entityConcepts = res;
      this.loadingEntityConcepts = false;
    });
  }

  getRelationConcepts(): void {
    this.loadingRelationConcepts = true;
    this.modelingService.getRelationConcepts(this.data.dataSource, this.entitySearchControl.value).subscribe((res) => {
      this.relationConcepts = res;
      this.loadingRelationConcepts = false;
    });
  }

  onIndexChange(event: MatTabChangeEvent): void {
    if (event.index !== 1) {
      this.modeling.relationConcept.next(null);
    }
  }

  toggleRelation(index: number, relationConcept: RelationConcept): void {
    const option = this.relationsList.options.toArray()[index];
    if (this.isSelected(relationConcept)) {
      option.toggle();
      this.modeling.relationConcept.next(null);
    } else {
      this.modeling.relationConcept.next(relationConcept);
    }
  }

  isSelected(relationConcept: RelationConcept): boolean {
    const current = this.modeling.relationConcept.getValue();
    return current === relationConcept;
  }

  newEntityConcept(): void {
    const ref = this.dialog.open(PlsNewEntityDialogComponent, {
      width: '650px',
      maxHeight: this.relativeTo.nativeElement.offsetHeight,
      data: this.data.dataSource
    });
    ref.afterClosed().subscribe(res => {
      if (res) {
        this.getEntityConcepts();
      }
    });
  }

  newRelationConcept(): void {
    const ref = this.dialog.open(PlsNewRelationDialogComponent, {
      width: '650px',
      maxHeight: this.relativeTo.nativeElement.offsetHeight,
      data: this.data.dataSource
    });
    ref.afterClosed().subscribe(res => {
      if (res) {
        this.getRelationConcepts();
      }
    });
  }

  setRecommendation(recommendation: DeltaModification): void {
    this.modeling.recommendation.next(recommendation);
  }
}
