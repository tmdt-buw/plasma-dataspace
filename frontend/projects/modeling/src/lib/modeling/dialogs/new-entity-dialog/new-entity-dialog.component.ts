import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { DataModelingControllerService, EntityTypeDTO } from '../../../api/generated/dms';

@Component({
  selector: 'pls-new-entity-dialog',
  templateUrl: './new-entity-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewEntityDialogComponent {

  form = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    sourceURI: new FormControl('')
  });

  error;

  constructor(public dialogRef: MatDialogRef<PlsNewEntityDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: string, private modelingService: DataModelingControllerService) {
  }

  submit(): void {
    this.error = null;
    this.modelingService.cacheEntityConcept(this.data, this.form.value as EntityTypeDTO).subscribe(() => this.dialogRef.close(true), error => this.error = error.error.message);
  }

}
