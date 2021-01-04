import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';


/**
 * Upload dialogue component
 */
@Component({
  selector: 'pls-upload-dialog',
  templateUrl: './data-source-dialog.component.html',
  styleUrls: ['./data-source-dialog.component.scss']
})
export class PlsDataSourceDialogComponent {

  constructor(private dialog: MatDialogRef<PlsDataSourceDialogComponent>) {
  }

  /**
   * Confirm close on active uploads
   */
  close(refresh: boolean): void {
    this.dialog.close(refresh);
  }

}
