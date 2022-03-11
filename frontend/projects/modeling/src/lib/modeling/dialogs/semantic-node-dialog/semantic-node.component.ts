import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import {
  Class,
  DataModelingControllerService,
  DeltaModification,
  Instance,
  NamedEntity,
  SemanticModelNode
} from '../../../api/generated/dms';
import { EditState } from './model/edit-state';

export interface PlsSemanticNodeDetailsData {
  modelId: string;
  node: SemanticModelNode;
  recommendations: DeltaModification[];
  examples: string[];
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
    switch (this.data.node._class) {
      case 'Class':
        const instance = this.getInstance();
        if (!instance) {
          return;
        }
        this.$edit.next(EditState.start);
        this.label = instance.label;
        this.description = instance.description;
        break;
      case 'NamedEntity':
        break;
      case 'Literal':
        break;
    }
  }

  acceptEdit(): void {
    this.$edit.next(EditState.confirm);
    const delta: DeltaModification = {
      entities: [this.data.node]
    };

    this.modelingService.modifyModel(this.data.modelId, delta).subscribe(() => null, () => {
      switch (this.data.node._class) {
        case 'Class':
          this.getInstance().label = this.label;
          this.getInstance().description = this.description;
          break;
      }
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

  getDescription(): string {
    switch (this.data.node._class) {
      case 'Class':
        return (this.data.node as Class).description;
      case 'NamedEntity':
        return (this.data.node as NamedEntity).description;
    }
    return 'No description available';
  }

  isNamedEntity(): boolean {
    return this.data.node._class === 'NamedEntity';
  }

  isClass(): boolean {
    return this.data.node._class === 'Class';
  }

  isMappedClass(): boolean {
    if (this.data.node._class !== 'Class') {
      return false;
    }
    const clazz = this.data.node as Class;
    if (!clazz.instance) {
      return false;
    }
    return !!clazz.instance.syntaxNodeUuid;
  }

  hasExampleValues(): boolean {
    if (!this.isMappedClass()) {
      return false;
    }
    // console.log('examples');
    return this.data.examples?.length >= 0;
  }

  isLiteral(): boolean {
    return this.data.node._class === 'Literal';
  }

  getInstance(): Instance {
    if (this.data.node._class !== 'Class') {
      return;
    }
    const clazz = this.data.node as Class;
    if (!clazz.instance) {
      return;
    }
    return clazz.instance;
  }

  getClassOrNamedEntity(): Class | NamedEntity {
    if (this.isClass()) {
      return this.data.node as Class;
    }
    if (this.isNamedEntity()) {
      return this.data.node as NamedEntity;
    }
  }

  customize(): void {
    console.log('start customization');
    if (!this.isClass()) {
      return;
    }
    if (this.getInstance()) {
      return;
    }
    (this.data.node as Class).instance = {
      label: this.data.node.label
    };
    this.startEdit();
  }

  hasRecommendations(): boolean {
    return this.isClass() && this.data.recommendations && this.data.recommendations.length > 0;
  }
}
