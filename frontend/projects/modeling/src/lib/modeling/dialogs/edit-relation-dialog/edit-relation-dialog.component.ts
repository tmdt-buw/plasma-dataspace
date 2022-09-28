import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { ModelingControllerService, ObjectProperty, Relation } from '../../../api/generated/dms';

@Component({
  selector: 'pls-edit-relation-dialog',
  templateUrl: './edit-relation-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss']
})
export class PlsNewRelationDialogComponent implements OnInit {

  @Input() modelId: string;
  @Input() defaultNamespace: string = 'local';
  @Input() editRelation: Relation;

  form = new FormGroup({
    label: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    namespace: new FormControl(this.defaultNamespace + ':', [Validators.required]),
    identifier: new FormControl(''),
    type: new FormControl('', [Validators.required])
  });

  error;
  relationType: 'ObjectProperty' | 'DataProperty';
  existingURI: string;

  constructor(private modalRef: NzModalRef, private modelingService: ModelingControllerService) {
  }

  ngOnInit(): void {
    if (this.editRelation) {
      this.existingURI = this.editRelation.uri;
      this.form.get('label').setValue(this.editRelation.label);
      this.form.get('description').setValue(this.editRelation.description);
      this.relationType = (this.editRelation._class === 'ObjectProperty') ? 'ObjectProperty' : 'DataProperty';
    }
  }

  submit(): void {
    this.error = null;
    const rel: Relation = {
      _class: this.form.get('type').value,
      uri: this.constructURI(),
      label: this.form.get('label').value,
      description: this.form.get('description').value
    };
    console.log(rel);
    this.modelingService.cacheRelation(this.modelId, rel).subscribe(() => this.modalRef.close(true), error => this.error = error.error.message);
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
