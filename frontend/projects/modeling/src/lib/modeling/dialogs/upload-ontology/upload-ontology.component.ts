import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { InlineObject, OntologyControllerService } from '../../../api/generated/kgs';
import { NzUploadFile } from 'ng-zorro-antd/upload';

@Component({
  selector: 'pls-upload-ontology',
  templateUrl: './upload-ontology.component.html',
  styleUrls: ['./upload-ontology.component.css']
})
export class UploadOntologyComponent {

  readonly multipartName = 'file';

  @Input() supportedFileFormats: string[];

// form
  form = new FormGroup({
    label: new FormControl('', [Validators.required, Validators.maxLength(6), Validators.minLength(2)]),
    uri: new FormControl('', [Validators.required]),
    prefix: new FormControl('', [Validators.required, Validators.maxLength(6), Validators.minLength(2)]),
    description: new FormControl('', [Validators.required]),
    filename: new FormControl()
  });
  error;

  ontologyFile: NzUploadFile;

  @Output() dataSourceCreated = new EventEmitter();

  @ViewChild('fileInput') fileInput: ElementRef;

  constructor(private ontologyController: OntologyControllerService) {
  }

  test: InlineObject;

  /**
   * Submits the ontology.
   */
  onSubmit(): void {
    this.error = null;
    this.ontologyController.addOntology(
      this.form.get('label').value,
      this.form.get('uri').value,
      this.form.get('prefix').value,
      {ontologyFile: this.ontologyFile.originFileObj}
    ).subscribe(success => console.log('upload ok'), error => this.error = error);
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    if (this.checkFileExtension(file.originFileObj)) {
      this.ontologyFile = file;
    } else {
      this.ontologyFile = undefined;
    }
    return false;
  }

  /**
   * Checks if the form is valid
   */
  isValid(): boolean {
    return this.form.valid && !!this.ontologyFile;
  }

  /**
   * checks the fileextension
   */
  checkFileExtension(file: File): boolean {
    const fileExtension = file.name.split('.').pop().toLowerCase();
    if (!this.supportedFileFormats || this.supportedFileFormats.includes(fileExtension)) {
      return true;
    } else {
      console.error('Unsupported file format');
      return false;
    }
  }
}

