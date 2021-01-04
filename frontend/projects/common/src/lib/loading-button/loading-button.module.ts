import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlsLoadingButtonComponent } from './loading-button/loading-button.component';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';


@NgModule({
  declarations: [PlsLoadingButtonComponent],
  imports: [
    CommonModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  exports: [PlsLoadingButtonComponent]
})
export class PlsLoadingButtonModule {
}
