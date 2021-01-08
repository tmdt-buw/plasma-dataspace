import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { DataModelingControllerService, RelationConceptDTO } from '../../../api/generated/dms';

@Component({
  selector: 'pls-new-relation-dialog',
  templateUrl: './new-relation-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewRelationDialogComponent {

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    sourceURI: new FormControl('')
  });

  error;

  constructor(public dialogRef: MatDialogRef<PlsNewRelationDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: string, private modelingService: DataModelingControllerService) {
  }

  submit(): void {
    this.error = null;
    this.modelingService.cacheRelationConcept(this.data, this.form.value as RelationConcept).subscribe(() => this.dialogRef.close(true), error => this.error = error.error.message);
  }

}
