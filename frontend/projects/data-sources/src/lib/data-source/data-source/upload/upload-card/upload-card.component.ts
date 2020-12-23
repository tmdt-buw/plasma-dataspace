import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Upload } from '../model/upload';

/**
 * Card to show upload progress
 */
@Component({
  selector: 'pls-upload-card',
  templateUrl: './upload-card.component.html',
  styleUrls: ['./upload-card.component.scss']
})
export class PlsUploadCardComponent {

  @Input() upload: Upload;

  @Output() retried = new EventEmitter();
  @Output() canceled = new EventEmitter();
  @Output() closed = new EventEmitter();

  constructor() {
  }
}
