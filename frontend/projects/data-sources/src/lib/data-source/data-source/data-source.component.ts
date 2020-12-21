import { HttpEvent, HttpEventType } from '@angular/common/http';
import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { CreateDataSourceDTO, DataSourceControllerService, DataSourceDTO, FileSystemControllerService } from '../../api/generated';
import { Upload } from './upload/model/upload';


@Component({
  selector: 'pls-data-source',
  templateUrl: './data-source.component.html',
  styleUrls: ['./data-source.component.scss'],
})
export class PlsDataSourceComponent {

  readonly multipartName = 'file';

  @Input() supportedFileFormats: string[];

  // form
  form = new FormGroup({
    title: new FormControl('', [Validators.required]),
    description: new FormControl('', [Validators.required]),
    longDescription: new FormControl(''),
    filename: new FormControl()
  });
  error;

  // upload queue
  uploads: Upload[] = [];

  @Output() dataSourceCreated = new EventEmitter();

  @ViewChild(PlsDataSourceComponent) dataSourceForm: PlsDataSourceComponent;
  @ViewChild('fileInput') fileInput: ElementRef;

  constructor(private dataSourceController: DataSourceControllerService, private fileSystemController: FileSystemControllerService) {
  }

  onSubmit(createDto: CreateDataSourceDTO): void {
    this.error = null;
    this.dataSourceController.createDataSource(createDto)
      .subscribe((res: DataSourceDTO) => {
        if ('uuid' in res) {
          this.form.reset();
          this.dataSourceCreated.emit();
        }
      }, error => this.error = error);
  }

  isValid(): boolean {
    return this.form.valid && this.uploads.every(upload => upload.status === 'finished');
  }

  /**
   * function handles drag and drop of file for upload
   * @event - catches the dropped file
   */
  dropped(files: File | File[]) {
    if (Array.isArray(files)) {
      // files.forEach(file => this.handleFile(file));
      console.error('Multiple files not supported');
    } else {
      this.handleFile(files);
    }
  }

  /**
   * function for select file for upload
   */
  select() {
    const files = this.fileInput.nativeElement.files;
    for (let i = 0; i < files.length; i++) {
      this.handleFile(files.item(i));
    }
  }

  /**
   * checks the fileextension
   */
  handleFile(file: File): void {
    const fileExtension = file.name.split('.').pop().toLowerCase();
    if (!this.supportedFileFormats || this.supportedFileFormats.includes(fileExtension)) {
      const upload = new Upload(file);
      // for multiple files: this.uploads.unshift(upload);
      this.uploads = [upload];
      this.uploadUpload(upload);
    } else {
      console.log('Unsupported file format');
    }
  }

  uploadUpload(upload: Upload) {
    upload.status = 'queued';
    this.fileSystemController.uploadFile(upload.file, 'events', true).subscribe((event: HttpEvent<any>) => {
      let percentDone;
      switch (event.type) {
        case HttpEventType.UploadProgress:
          percentDone = Math.round(100 * event.loaded / event.total);
          upload.progress = percentDone;
          upload.loaded = event.loaded;
          break;
        case HttpEventType.Response:
          upload.progress = 100;
          upload.status = 'finished';
          this.form.get('filename').setValue(event.body);
          break;
      }
    }, error => {
      upload.status = 'error';
      upload.error = error.statusText;
    });
  }

  /**
   * Cancel an upload
   * @param upload - upload to cancel
   */
  onCancel(upload: Upload) {
    upload.upload.unsubscribe();
    upload.status = 'canceled';
  }

  /**
   * Close upload card
   * @param upload - card to close
   */
  onClose(upload: Upload) {
    if (upload.status !== 'queued') {
      this.uploads.splice(this.uploads.indexOf(upload), 1);
      this.form.get('filename').reset();
    }
  }
}
