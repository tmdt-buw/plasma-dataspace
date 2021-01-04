import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlsJsonViewerComponent } from './json-viewer/json-viewer/json-viewer.component';
import { CdkTreeModule } from '@angular/cdk/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { FlexLayoutModule } from '@angular/flex-layout';


@NgModule({
  declarations: [PlsJsonViewerComponent],
  imports: [
    CommonModule,
    CdkTreeModule,
    FlexLayoutModule,
    MatButtonModule,
    MatIconModule
  ],
  exports: [PlsJsonViewerComponent]
})
export class JsonViewerModule {
}
