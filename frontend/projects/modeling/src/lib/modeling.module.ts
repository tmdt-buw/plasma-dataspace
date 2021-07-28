import { DragDropModule } from '@angular/cdk/drag-drop';
import { FullscreenOverlayContainer, OverlayContainer, OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { ApiModule, BASE_PATH } from './api/generated/dms';
import { ApiModule as KGSApiModule } from './api/generated/kgs';
import { PlsEntityConceptMenuComponent } from './modeling/context-menu/entity-concept-menu/entity-concept-menu.component';
import { PlsRelationConceptMenuComponent } from './modeling/context-menu/relation-concept-menu/relation-concept-menu.component';
import { PlsDiagramComponent } from './modeling/diagram/diagram.component';
import { InlineInputEditComponent } from './modeling/dialogs/common/inline-edit/inline-edit.component';
import { PlsConceptDialogComponent } from './modeling/dialogs/concept-dialog/concept-dialog.component';
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
    PlsNewRelationDialogComponent, PlsImportDialogComponent,
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
    ReactiveFormsModule,
    OverlayModule,
    NzButtonModule,
    NzIconModule,
    NzToolTipModule,
    NzSwitchModule,
    NzFormModule,
    NzInputModule,
    NzDropDownModule,
    NzSelectModule,
    NzSpinModule,
    NzCardModule,
    NzDividerModule,
    NzTypographyModule,
    NzUploadModule,
    NzModalModule,
    NzTabsModule,
    NzAlertModule
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
