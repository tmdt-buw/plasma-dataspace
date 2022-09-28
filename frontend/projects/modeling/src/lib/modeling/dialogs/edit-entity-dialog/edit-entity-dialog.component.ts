import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Class, ModelingControllerService, NamedEntity } from '../../../api/generated/dms';

@Component({
  selector: 'pls-edit-entity-dialog',
  templateUrl: './edit-entity-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewEntityDialogComponent implements OnInit {

  @Input() modelId: string;
  @Input() defaultNamespace: string = 'local';
  @Input() editNode: Class | NamedEntity;

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    namespace: new FormControl(this.defaultNamespace + ':', [Validators.required]),
    identifier: new FormControl(''),
    namedEntity: new FormControl(false)
  });

  error;
  existingURI: string;

  constructor(private modalRef: NzModalRef, private modelingService: ModelingControllerService) {
  }

  ngOnInit(): void {
    if (this.editNode) {
      this.existingURI = this.editNode.uri;
      this.form.get('label').setValue(this.editNode.label);
      this.form.get('description').setValue(this.editNode.description);
      this.form.get('namedEntity').setValue(this.editNode._class === 'NamedEntity');
    }
  }


  submit(): void {
    this.error = null;

    if (this.form.get('label').value.trim() === '') {
      this.error = 'Empty label is not allowed';
      return;
    }
    let node: Class | NamedEntity;
    if (this.form.get('namedEntity').value) {
      node = {
        _class: 'NamedEntity',
        uri: this.constructURI(),
        description: this.form.get('description').value,
        label: this.form.get('label').value
      };
    } else {
      node = {
        _class: 'Class',
        uri: this.constructURI(),
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

  generateIdentifier(): string {
    return this.convertLabelToIdentifier(this.form.get('label').value);
  }

  convertLabelToIdentifier(input: string): string {
    return input.trim().replace(/[^a-zA-Z0-9]/g, '_');
  }

  constructURI(): string {
    let namespace = this.form.get('namespace').value;
    let identifier = this.form.get('identifier').value;

    if (this.existingURI && !identifier) {
      return this.existingURI;
    }
    if (!namespace.includes(':')) {
      namespace = namespace + ':';
    }
    if (!identifier) {
      identifier = this.generateIdentifier();
    }
    return namespace + identifier;
  }

  onIdentifierEdited(event: Event): void {
  }

  onNamespaceEdited(event: Event): void {
    let namespace = this.form.get('namespace').value;

    if (!namespace.includes(':')) {
      namespace = namespace + ':';
    }
    this.form.get('namespace').setValue(namespace);
  }

  onLabelEdited(event: Event): void {
  }


}
