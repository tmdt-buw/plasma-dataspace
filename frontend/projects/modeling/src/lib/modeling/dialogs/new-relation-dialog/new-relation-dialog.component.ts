import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { DataModelingControllerService, RelationConcept } from '../../../api/generated/dms';

@Component({
  selector: 'pls-new-relation-dialog',
  templateUrl: './new-relation-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewRelationDialogComponent {

  @Input() dataSource: string;

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    sourceURI: new FormControl('')
  });

  error;
  defaultConceptPrefix = 'http://plasma/';

  constructor(private modalRef: NzModalRef, private modelingService: DataModelingControllerService) {
  }

  submit(): void {
    this.error = null;
    if (this.form.get('sourceURI').value === '') {
      const defaultUri = this.defaultConceptPrefix + this.form.get('label').value;
      this.form.get('sourceURI').setValue(defaultUri);
    }
    this.modelingService.cacheRelationConcept(this.dataSource, this.form.value as RelationConcept).subscribe(() => this.modalRef.close(true), error => this.error = error.error.message);
  }

  cancel(): void {
    this.error = null;
    this.modalRef.close();
  }

}
