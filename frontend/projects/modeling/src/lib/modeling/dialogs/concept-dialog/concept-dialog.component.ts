import { Component, Input, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ModalOptions, NzModalService } from 'ng-zorro-antd/modal';
import { NzTabChangeEvent } from 'ng-zorro-antd/tabs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  DataModelingControllerService,
  DeltaModification,
  Relation,
  SemanticModelNode
} from '../../../api/generated/dms';
import { ModelingService } from '../../modeling.service';
import { ModalBaseConfig } from '../common/modal.base-config';
import { PlsNewEntityDialogComponent } from '../new-entity-dialog/new-entity-dialog.component';
import { PlsNewRelationDialogComponent } from '../new-relation-dialog/new-relation-dialog.component';


@Component({
  selector: 'pls-concept-dialog',
  templateUrl: 'concept-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'concept-dialog.component.less'],
  encapsulation: ViewEncapsulation.None
})
export class PlsConceptDialogComponent implements OnInit, OnDestroy {

  @Input() dataSource: string;

  @ViewChild('colorEl') color;

  entitySearchControl = new FormControl('');
  relationSearchControl = new FormControl('');

  loadingEntityConcepts = true;
  loadingRelationConcepts = true;

  semanticModelNodes: SemanticModelNode[] = [];
  relations: Relation[] = [];

  // TODO find useful recommendation view
  constructor(private modeling: ModelingService, private modelingService: DataModelingControllerService, private modal: NzModalService) {

  }

  ngOnInit(): void {
    this.entitySearchControl.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => this.getSemanticNodeTemplates());
    this.relationSearchControl.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => this.getRelationTemplates());
    this.getSemanticNodeTemplates();
    this.getRelationTemplates();
  }

  ngOnDestroy(): void {
    // remove any selected relation concept
    this.modeling.relation.next(null);
  }

  getSemanticNodeTemplates(): void {
    this.loadingEntityConcepts = true;
    this.modelingService.getElements(this.dataSource, this.entitySearchControl.value).subscribe((res) => {
      this.semanticModelNodes = res;
      this.loadingEntityConcepts = false;
    });
  }

  getRelationTemplates(): void {
    this.loadingRelationConcepts = true;
    this.modelingService.getRelations(this.dataSource, this.relationSearchControl.value).subscribe((res) => {
      this.relations = res;
      this.loadingRelationConcepts = false;
    });
  }

  onIndexChange(event: NzTabChangeEvent): void {
    if (event.index !== 1) {
      this.modeling.relation.next(null);
    }
  }

  toggleRelation(index: number, relation: Relation): void {
    console.log('toggle relation', index, relation);
    if (this.isSelected(relation)) {
      this.modeling.relation.next(null);
    } else {
      this.modeling.relation.next(relation);
    }
  }

  isSelected(relation: Relation): boolean {
    const current = this.modeling.relation.getValue();
    return current === relation;
  }

  newSemanticNode(): void {
    const config: ModalOptions = {
      nzTitle: 'Add new modeling element',
      nzContent: PlsNewEntityDialogComponent,
      nzComponentParams: {
        dataSource: this.dataSource
      }
    };
    const ref = this.modal.create(Object.assign(config, ModalBaseConfig));
    ref.afterClose.subscribe(res => {
      if (res) {
        this.getSemanticNodeTemplates();
      }
    });
  }

  newRelationConcept(): void {
    const config: ModalOptions = {
      nzTitle: 'Add new relation',
      nzContent: PlsNewRelationDialogComponent,
      nzComponentParams: {
        dataSource: this.dataSource
      }
    };
    const ref = this.modal.create(Object.assign(config, ModalBaseConfig));
    ref.afterClose.subscribe(res => {
      if (res) {
        this.getRelationTemplates();
      }
    });
  }

  setRecommendation(recommendation: DeltaModification): void {
    this.modeling.recommendation.next(recommendation);
  }
}
