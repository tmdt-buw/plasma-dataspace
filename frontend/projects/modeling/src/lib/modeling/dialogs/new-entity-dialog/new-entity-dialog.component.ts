import { Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { DataModelingControllerService, EntityConcept } from '../../../api/generated/dms';

@Component({
  selector: 'pls-new-entity-dialog',
  templateUrl: './new-entity-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewEntityDialogComponent {

  @Input() dataSource: string;

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    sourceURI: new FormControl('')
  });

  error;
  defaultConceptPrefix = 'plasma:';

  constructor(private modalRef: NzModalRef, private modelingService: DataModelingControllerService) {
  }

  submit(): void {
    this.error = null;

    if (this.form.get('label').value.trim() === '') {
      this.error = 'Empty label is not allowed';
      return;
    }
    if (this.form.get('sourceURI').value === '') {
      const defaultUri = this.cleanURI(this.defaultConceptPrefix + this.form.get('label').value.trim().split(' ').join('_'));
      this.form.get('sourceURI').setValue(defaultUri);
    } else if (this.form.get('sourceURI').value.startsWith(':')) {
      const defaultUri = this.cleanURI(this.defaultConceptPrefix + this.form.get('label').value.trim().split(' ').join('_'));
      this.form.get('sourceURI').setValue(defaultUri);
    } else {
      this.form.get('sourceURI').setValue(this.cleanURI(this.form.get('sourceURI').value));
    }
    this.modelingService.cacheEntityConcept(this.dataSource, this.form.value as EntityConcept).subscribe(() => this.modalRef.close(true), error => this.error = error.error.message);
  }

  cancel(): void {
    this.error = null;
    this.modalRef.close();
  }

  generateSourceURI(): string {
    return this.cleanURI('plasma:' + this.form.get('label').value);
  }

  cleanURI(input: string): string {
    return input.trim().split(' ').join('_');
  }
}
