import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CombinedModel, DeltaModification, ModelingControllerService } from '../../../api/generated/dms';
import { FormBuilder } from '@angular/forms';
import { PlsSemanticNodeDetailsData } from '../semantic-node-dialog/semantic-node.component';

@Component({
  selector: 'pls-model-recommendation-dialog',
  templateUrl: './model-recommendation-dialog.component.html',
  styleUrls: ['./model-recommendation-dialog.component.less']
})
export class ModelRecommendationDialogComponent implements OnInit {

  recommendations: DeltaModification[];

  @Input() combinedModel: CombinedModel;

  @Output()
  public recommendationSelect: EventEmitter<DeltaModification> = new EventEmitter();
  @Output()
  public recommendationAccept: EventEmitter<DeltaModification> = new EventEmitter();
  @Output()
  public recommendationReject: EventEmitter<DeltaModification> = new EventEmitter();
  constructor(private modelingService: ModelingControllerService, private fb: FormBuilder) { }

  ngOnInit(): void {
  }


  showRecommendation(recommendation: DeltaModification): void {
    // console.log('emitting select for recommendation', recommendation);
    this.recommendationSelect.emit(recommendation);
  }

  hideRecommendation(): void {
    this.recommendationSelect.emit(null);
  }

  acceptRecommendation(recommendation: DeltaModification): void {
    this.recommendationAccept.emit(recommendation);
    const index = this.combinedModel.recommendations.indexOf(recommendation, 0);
    if (index > -1) {
      this.combinedModel.recommendations.splice(index, 1);
    }
  }

  rejectRecommendation(recommendation: DeltaModification): void {
    // nyi
  }
}
