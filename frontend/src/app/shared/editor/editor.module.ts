import { NgModule } from '@angular/core';
import { EditorComponent } from './editor/editor.component';
import { EditorContentBodyDirective } from './directives/editor-content-body.directive';
import { EditorContentHeaderButtonsDirective } from './directives/editor-content-header-buttons.directive';
import { EditorContentHeaderSubtextDirective } from './directives/editor-content-header-subtext.directive';
import { EditorContentHeaderTextDirective } from './directives/editor-content-header-text.directive';
import { CommonModule } from '@angular/common';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CdkScrollableModule } from '@angular/cdk/scrolling';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@NgModule({
  declarations: [
    EditorComponent,
    EditorContentBodyDirective,
    EditorContentHeaderButtonsDirective,
    EditorContentHeaderSubtextDirective,
    EditorContentHeaderTextDirective
  ],
  imports: [
    CdkScrollableModule,
    CommonModule,
    FlexLayoutModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  exports: [
    EditorComponent,
    EditorContentBodyDirective,
    EditorContentHeaderButtonsDirective,
    EditorContentHeaderSubtextDirective,
    EditorContentHeaderTextDirective
  ]
})
export class EditorModule {
}
