import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SyntacticOperationDTO } from '../../../../api/generated/dms';

export interface PlsOperationsDialogData {
  operation: SyntacticOperationDTO;
}

@Component({
  selector: 'pls-operations-dialog',
  templateUrl: './operations-dialog.component.html',
  styleUrls: ['../../common/dialog.styles.scss']
})
export class PlsOperationsDialogComponent {

  error;

  constructor(public dialogRef: MatDialogRef<PlsOperationsDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: PlsOperationsDialogData) {
  }

  apply(): void {
    this.dialogRef.close(this.data);
  }
}
