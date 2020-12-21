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

  @Output() retry = new EventEmitter();
  @Output() cancel = new EventEmitter();
  @Output() close = new EventEmitter();

  constructor() {
  }
}
