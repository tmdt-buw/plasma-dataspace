import { Component, Input } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { DataModelingControllerService, EntityType } from '../../../api/generated/dms';
import { EditState } from './model/edit-state';

export interface PlsSemanticNodeDetailsData {
  dataSource: string;
  node: EntityType;
}

@Component({
  selector: 'pls-semantic-node-details',
  templateUrl: 'semantic-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSemanticNodeDetailsComponent {

  @Input() data: PlsSemanticNodeDetailsData;

  $edit: BehaviorSubject<EditState> = new BehaviorSubject<EditState>(EditState.confirm);
  label: string;
  description: string;

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
}
