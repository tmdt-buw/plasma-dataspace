import { DragDropModule } from '@angular/cdk/drag-drop';
import { FullscreenOverlayContainer, OverlayContainer, OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiModule, BASE_PATH } from './api/generated/dms';
import { ApiModule as KGSApiModule } from './api/generated/kgs';
import { PlsEntityConceptMenuComponent } from './modeling/context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from './modeling/context-menu/relation-concept-menu/relation-concept-menu.component';
import { PlsDiagramComponent } from './modeling/diagram/diagram.component';
import { InlineInputEditComponent } from './modeling/dialogs/common/inline-edit/inline-edit.component';
import { PlsConceptDialogComponent } from './modeling/dialogs/concept-dialog/concept-dialog.component';
import { PlsFileDropDirective } from './modeling/dialogs/import-dialog/directives/file-drop.directive';
import { PlsImportDialogComponent } from './modeling/dialogs/import-dialog/import-dialog.component';
import { PlsNewEntityDialogComponent } from './modeling/dialogs/new-entity-dialog/new-entity-dialog.component';
import { PlsNewRelationDialogComponent } from './modeling/dialogs/new-relation-dialog/new-relation-dialog.component';
import { PlsOperationsDialogModule } from './modeling/dialogs/operations-dialog/operations-dialog.module';
import { PlsSemanticNodeDetailsComponent } from './modeling/dialogs/semantic-node-dialog/semantic-node.component';
import { PlsSyntaxNodeDetailsComponent } from './modeling/dialogs/syntax-node-dialog/syntax-node.component';
import { PlsFullscreenDirective } from './modeling/directives/fullscreen.directive';
import { PlsModelingComponent } from './modeling/modeling.component';


@NgModule({
  declarations: [
    PlsModelingComponent, PlsDiagramComponent, PlsSyntaxNodeDetailsComponent,
    PlsSemanticNodeDetailsComponent, PlsConceptDialogComponent, PlsNewEntityDialogComponent,
    PlsNewRelationDialogComponent, PlsImportDialogComponent, PlsFileDropDirective,
    InlineInputEditComponent, PlsEntityConceptMenuComponent, PlsRelationConceptMenuComponent, PlsFullscreenDirective
  ],
  imports: [
    // Plasma
    ApiModule,
    PlsOperationsDialogModule,
    KGSApiModule,
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
  exports: [PlsModelingComponent]
})
export class PlsModelingModule {
  public static setBasePath(basePath: string): ModuleWithProviders<PlsModelingModule> {
    return {
      ngModule: PlsModelingModule,
      providers: [
        {provide: BASE_PATH, useValue: basePath},
        // enable correct display of cdk overlay elements like dialogs in fullscreen mode
        {provide: OverlayContainer, useClass: FullscreenOverlayContainer}
      ]
    };
  }
}
