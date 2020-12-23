import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlsFileDropDirective } from './directive/file-drop.directive';


@NgModule({
  declarations: [PlsFileDropDirective],
  imports: [
    CommonModule
  ],
  exports: [PlsFileDropDirective]
})
export class FileDropModule {
}
