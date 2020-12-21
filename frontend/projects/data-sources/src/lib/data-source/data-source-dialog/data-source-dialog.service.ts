import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PlsDataSourceDialogComponent } from './data-source-dialog.component';
import { Observable } from 'rxjs';

/**
 * Service that shows the upload dialogue for easy use
 */
@Injectable()
export class PlsUploadDialogService {

  constructor(private dialog: MatDialog) {
  }

  /**
   * Shows an upload dialog allowing file upload.
   */
  show(): Observable<any> {
    return this.dialog.open(PlsDataSourceDialogComponent, {
      width: '70%'
    }).afterClosed();
  }
}
