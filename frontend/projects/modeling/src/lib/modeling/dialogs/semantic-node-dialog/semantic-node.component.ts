import { Component, ElementRef, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DataModelingControllerService, EntityType } from '../../../api/generated/dms';

export interface PlsSemanticNodeDetailsData {
  dataSource: string;
  node: EntityTypeDTO;
  relativeTo: ElementRef;
}

@Component({
  selector: 'pls-semantic-node-details',
  templateUrl: 'semantic-node.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsSemanticNodeDetailsComponent implements OnInit {

  edit: boolean = false;
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
    this.edit = true;
    this.label = this.data.node.label;
    this.description = this.data.node.description;
  }

  acceptEdit(): void {
    this.edit = false;
    this.modelingService.updateEntityType(this.data.dataSource, this.data.node.uuid, this.data.node).subscribe(() => null, () => {
      this.data.node.label = this.label;
      this.data.node.description = this.description;
    });
  }
}
