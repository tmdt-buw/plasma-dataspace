import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { DataModelingControllerService, ObjectProperty } from '../../../api/generated/dms';

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
    uri: new FormControl(''),
    type: new FormControl('', [Validators.required])
  });

  error;
  defaultPrefix = 'plasma:';
  relationType: 'ObjectProperty' | 'DataProperty';

  constructor(private modalRef: NzModalRef, private modelingService: DataModelingControllerService) {
  }

  submit(): void {
    this.error = null;
    if (this.form.get('uri').value === '') {
      const defaultUri = this.cleanURI(this.defaultPrefix + this.form.get('label').value);
      this.form.get('uri').setValue(defaultUri);
    } else if (this.form.get('uri').value.startsWith(':')) {
      const defaultUri = this.cleanURI(this.defaultPrefix + this.form.get('label').value);
      this.form.get('uri').setValue(defaultUri);
    } else {
      this.form.get('uri').setValue(this.cleanURI(this.form.get('uri').value));
    }
    const rel: ObjectProperty = {
      _class: this.form.get('type').value,
      uri: this.form.get('uri').value,
      label: this.form.get('label').value
    };
    console.log(rel);
    this.modelingService.cacheRelation(this.dataSource, rel).subscribe(() => this.modalRef.close(true), error => this.error = error.error.message);
  }

  cancel(): void {
    this.error = null;
    this.modalRef.close();
  }

  generateURI(): string {
    return this.cleanURI(this.defaultPrefix + this.form.get('label').value);
  }

  cleanURI(input: string): string {
    return input.trim().split(' ').join('_');
  }

}
