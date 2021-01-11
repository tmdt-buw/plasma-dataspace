import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PlsCheckboxFormElementComponent } from './schema-form/elements/checkbox-form-element.component';
import { PlsComplexFormElementComponent } from './schema-form/elements/complex-form-element.component';
import { PlsFormElementComponent } from './schema-form/elements/form-element.component';
import { PlsHiddenFormElementComponent } from './schema-form/elements/hidden-form-element.component';
import { PlsTextFormElementComponent } from './schema-form/elements/text-form-element.component';
import { PlsSchemaFormComponent } from './schema-form/schema-form.component';
import { MatButtonModule } from '@angular/material/button';

@NgModule({
  declarations: [
    PlsFormElementComponent,
    PlsCheckboxFormElementComponent,
    PlsComplexFormElementComponent,
    PlsTextFormElementComponent,
    PlsHiddenFormElementComponent,
    PlsSchemaFormComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule
  ],
  exports: [
    PlsSchemaFormComponent
  ]
})

export class PlsSchemaFormModule {
}
