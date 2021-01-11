import { Component, Input } from '@angular/core';
import { TypeDefinitionDTOObject } from '../../../../../../api/generated/dms';

enum FormElementTypes {
  Text,
  Hidden,
  Checkbox,
  Complex
}

@Component({
  selector: 'pls-schema-form-element',
  templateUrl: 'form-element.component.html'
})
export class PlsFormElementComponent {

  public readonly FormElementTypes = FormElementTypes;

  @Input()
  public formElement: TypeDefinitionDTOObject;

  public getFormElement(): FormElementTypes {
    if (this.formElement.hidden) {
      return FormElementTypes.Hidden;
    }

    switch (this.formElement.type) {
      case 'Complex':
        return FormElementTypes.Complex;
      case 'DataType':
        return FormElementTypes.Checkbox;
      default:
        return FormElementTypes.Text;
    }
  }

  public getCustomConfiguration(): any {
    switch (this.formElement.type) {
      case 'DataType':
        return {
          values: [
            'Unknown',
            'String',
            'Boolean',
            'Number',
            'Binary'
          ]
        };
      default:
        return {};
    }
  }

}
