import {Component, ElementRef, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {DataModelingControllerService, EntityType} from '../../../api/generated/dms';
import {BehaviorSubject} from 'rxjs';
import {EditState} from './model/edit-state';

export interface PlsSemanticNodeDetailsData {
  dataSource: string;
  node: EntityType;
  relativeTo: ElementRef;
}

@Component({
  selector: 'pls-semantic-node-details',
  templateUrl: 'semantic-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSemanticNodeDetailsComponent implements OnInit {

  $edit: BehaviorSubject<EditState> = new BehaviorSubject<EditState>(EditState.confirm);
  label: string;
  description: string;

  private relativeTo: ElementRef;

  constructor(public dialogRef: MatDialogRef<PlsSemanticNodeDetailsComponent>, @Inject(MAT_DIALOG_DATA) public data: PlsSemanticNodeDetailsData, private modelingService: DataModelingControllerService) {
    this.relativeTo = data.relativeTo;
  }

  ngOnInit(): void {
    const rect: DOMRect = this.relativeTo.nativeElement.getBoundingClientRect();
    const position = {top: `${rect.top + 15}px`, right: `${rect.left + 15}px`};
    this.dialogRef.updatePosition(position);
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
