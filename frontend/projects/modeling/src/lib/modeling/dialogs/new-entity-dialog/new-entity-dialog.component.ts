import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Class, ModelingControllerService, NamedEntity } from '../../../api/generated/dms';

@Component({
  selector: 'pls-new-entity-dialog',
  templateUrl: './new-entity-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewEntityDialogComponent {

  @Input() modelId: string;
  @Input() defaultNamespace: string = 'local';

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    uri: new FormControl(''),
    namedEntity: new FormControl(false)
  });

  error;
  isNamedEntity = false;

  constructor(private modalRef: NzModalRef, private modelingService: ModelingControllerService) {
  }

  submit(): void {
    this.error = null;

    if (this.form.get('label').value.trim() === '') {
      this.error = 'Empty label is not allowed';
      return;
    }
    if (this.form.get('uri').value === '') {
      const defaultUri = this.cleanURI(this.defaultNamespace + ':' + this.form.get('label').value);
      this.form.get('uri').setValue(defaultUri);
    } else if (this.form.get('uri').value.startsWith(':')) {
      const defaultUri = this.cleanURI(this.defaultNamespace + ':' + this.form.get('label').value);
      this.form.get('uri').setValue(defaultUri);
    } else {
      this.form.get('uri').setValue(this.cleanURI(this.form.get('uri').value));
    }
    let node: Class | NamedEntity;
    if (this.form.get('namedEntity').value) {
      node = {
        _class: 'NamedEntity',
        uri: this.form.get('uri').value,
        description: this.form.get('description').value,
        label: this.form.get('label').value
      };
    } else {
      node = {
        _class: 'Class',
        uri: this.form.get('uri').value,
        description: this.form.get('description').value,
        label: this.form.get('label').value
      };
    }
    this.modelingService.cacheElement(this.modelId, node).subscribe(() => this.modalRef.close(true), error => this.error = error.error.message);
  }

  cancel(): void {
    this.error = null;
    this.modalRef.close();
  }

  generateURI(): string {
    return this.cleanURI(this.defaultNamespace + ':' + this.form.get('label').value);
  }

  cleanURI(input: string): string {
    return input.trim().split(' ').join('_');
  }

  onURIEdited(event: Event): void {
    console.log('uri edited', event);
  }
}
