import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ModalOptions, NzModalService } from 'ng-zorro-antd/modal';
import { NzTabChangeEvent } from 'ng-zorro-antd/tabs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  DataModelingControllerService,
  DeltaModification,
  EntityConcept,
  RelationConcept
} from '../../../api/generated/dms';
import { ModelingService } from '../../modeling.service';
import { ModalBaseConfig } from '../common/modal.base-config';
import { PlsNewEntityDialogComponent } from '../new-entity-dialog/new-entity-dialog.component';
import { PlsNewRelationDialogComponent } from '../new-relation-dialog/new-relation-dialog.component';


@Component({
  selector: 'pls-concept-dialog',
  templateUrl: 'concept-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'concept-dialog.component.less']
})
export class PlsConceptDialogComponent implements OnInit, OnDestroy {

  @Input() dataSource: string;

  @ViewChild('colorEl') color;

  entitySearchControl = new FormControl('');
  relationSearchControl = new FormControl('');

  loadingEntityConcepts = true;
  loadingRelationConcepts = true;

  entityConcepts: EntityConcept[] = [];
  relationConcepts: RelationConcept[] = [];

  // TODO find useful recommendation view
  constructor(private modeling: ModelingService, private modelingService: DataModelingControllerService, private modal: NzModalService) {

  }

  ngOnInit(): void {
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

  ngOnDestroy(): void {
    // remove any selected relation concept
    this.modeling.relationConcept.next(null);
  }

  getEntityConcepts(): void {
    this.loadingEntityConcepts = true;
    this.modelingService.getEntityConcepts(this.dataSource, this.entitySearchControl.value).subscribe((res) => {
      this.entityConcepts = res;
      this.loadingEntityConcepts = false;
    });
  }

  getRelationConcepts(): void {
    this.loadingRelationConcepts = true;
    this.modelingService.getRelationConcepts(this.dataSource, this.relationSearchControl.value).subscribe((res) => {
      this.relationConcepts = res;
      this.loadingRelationConcepts = false;
    });
  }

  onIndexChange(event: NzTabChangeEvent): void {
    if (event.index !== 1) {
      this.modeling.relationConcept.next(null);
    }
  }

  toggleRelation(index: number, relationConcept: RelationConcept): void {
    if (this.isSelected(relationConcept)) {
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
    const config: ModalOptions = {
      nzTitle: 'New entity concept',
      nzContent: PlsNewEntityDialogComponent,
      nzComponentParams: {
        dataSource: this.dataSource
      }
    };
    const ref = this.modal.create(Object.assign(config, ModalBaseConfig));
    ref.afterClose.subscribe(res => {
      if (res) {
        this.getEntityConcepts();
      }
    });
  }

  newRelationConcept(): void {
    const config: ModalOptions = {
      nzTitle: 'New relation concept',
      nzContent: PlsNewRelationDialogComponent,
      nzComponentParams: {
        dataSource: this.dataSource
      }
    };
    const ref = this.modal.create(Object.assign(config, ModalBaseConfig));
    ref.afterClose.subscribe(res => {
      if (res) {
        this.getRelationConcepts();
      }
    });
  }

  setRecommendation(recommendation: DeltaModification): void {
    this.modeling.recommendation.next(recommendation);
  }
}
