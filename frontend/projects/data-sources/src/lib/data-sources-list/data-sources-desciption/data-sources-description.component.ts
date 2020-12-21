import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'pls-data-sources-description',
  templateUrl: './data-sources-description.component.html'
})
export class PlsDataSourcesDescriptionComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: { title: string, text: string }) {
  }
}
