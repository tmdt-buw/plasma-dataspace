import { ModuleWithProviders, NgModule } from '@angular/core';
import { PlsModelingComponent } from './modeling/modeling.component';
import { ApiModule, BASE_PATH } from './api/generated/dms';
import { CommonModule } from '@angular/common';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { MatListModule } from '@angular/material/list';
import { PlsDiagramComponent } from './modeling/diagram/diagram.component';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { PlsSyntaxNodeDetailsComponent } from './modeling/dialogs/syntax-node-dialog/syntax-node.component';
import { PlsSemanticNodeDetailsComponent } from './modeling/dialogs/semantic-node-dialog/semantic-node.component';
import { PlsConceptDialogComponent } from './modeling/dialogs/concept-dialog/concept-dialog.component';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PlsNewEntityDialogComponent } from './modeling/dialogs/new-entity-dialog/new-entity-dialog.component';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { PlsNewRelationDialogComponent } from './modeling/dialogs/new-relation-dialog/new-relation-dialog.component';
import { PlsImportDialogComponent } from './modeling/dialogs/import-dialog/import-dialog.component';
import { PlsFileDropDirective } from './modeling/dialogs/import-dialog/directives/file-drop.directive';
import { InlineInputEditComponent } from './modeling/dialogs/common/inline-edit/inline-edit.component';
import { OverlayModule } from '@angular/cdk/overlay';
import { PlsEntityConceptMenuComponent } from './modeling/context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from './modeling/context-menu/relation-concept-menu/relation-concept-menu.component';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';


@NgModule({
  declarations: [
    PlsModelingComponent, PlsDiagramComponent, PlsSyntaxNodeDetailsComponent,
    PlsSemanticNodeDetailsComponent, PlsConceptDialogComponent, PlsNewEntityDialogComponent,
    PlsNewRelationDialogComponent, PlsImportDialogComponent, PlsFileDropDirective,
    InlineInputEditComponent, PlsEntityConceptMenuComponent, PlsRelationConceptMenuComponent
  ],
  imports: [
    // Plasma
    ApiModule,
    // Angular
    CommonModule,
    DragDropModule,
    FlexLayoutModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatFormFieldModule,
    MatMenuModule,
    MatSelectModule,
    MatTabsModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
    OverlayModule
  ],
  entryComponents: [
    PlsSyntaxNodeDetailsComponent, PlsSemanticNodeDetailsComponent, PlsConceptDialogComponent,
    PlsNewEntityDialogComponent, PlsNewRelationDialogComponent, PlsImportDialogComponent,
    PlsEntityConceptMenuComponent, PlsRelationConceptMenuComponent
  ],
  exports: [PlsModelingComponent]
})
export class PlsModelingModule {
  public static setBasePath(basePath: string): ModuleWithProviders<PlsModelingModule> {
    return {
      ngModule: PlsModelingModule,
      providers: [{provide: BASE_PATH, useValue: basePath}]
    };
  }
}
