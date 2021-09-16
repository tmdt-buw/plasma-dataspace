import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { DataModelingControllerService, DeltaModification, EntityType } from '../../../api/generated/dms';
import { EditState } from './model/edit-state';

export interface PlsSemanticNodeDetailsData {
  dataSource: string;
  node: EntityType;
  recommendations: DeltaModification[];
}

@Component({
  selector: 'pls-semantic-node-details',
  templateUrl: 'semantic-node.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'semantic-node.component.less']
})
export class PlsSemanticNodeDetailsComponent {

  @Input() data: PlsSemanticNodeDetailsData;

  $edit: BehaviorSubject<EditState> = new BehaviorSubject<EditState>(EditState.confirm);
  label: string;
  description: string;

  public output = new Subject<DeltaModification>();

  @Output()
  public recommendationSelect: EventEmitter<DeltaModification> = new EventEmitter();
  @Output()
  public recommendationAccept: EventEmitter<DeltaModification> = new EventEmitter();
  @Output()
  public recommendationReject: EventEmitter<DeltaModification> = new EventEmitter();

  constructor(private modelingService: DataModelingControllerService) {

  }

  startEdit(): void {
    this.$edit.next(EditState.start);
    this.label = this.data.node.label;
    this.description = this.data.node.description;
  }

  acceptEdit(): void {
    this.$edit.next(EditState.confirm);
    this.modelingService.updateEntityType(this.data.dataSource, this.data.node.uuid, this.data.node).subscribe(() => null, () => {
      this.data.node.label = this.label;
      this.data.node.description = this.description;
    });
  }

  cancelEdit(): void {
    this.$edit.next(EditState.cancel);
  }

  get editing(): boolean {
    return this.$edit.getValue() === EditState.start;
  }

  showRecommendation(recommendation: DeltaModification): void {
    console.log('emitting select for recommendation', recommendation);
    this.recommendationSelect.emit(recommendation);
  }

  hideRecommendation(): void {
    this.recommendationSelect.emit(null);
  }

  acceptRecommendation(recommendation: DeltaModification): void {
    this.recommendationAccept.emit(recommendation);
  }

  rejectRecommendation(recommendation: DeltaModification): void {
    // nyi
  }
}
