import { Subject } from 'rxjs';
import { UriGeneratorComponent } from './uri-generator.component';

describe('Semantic URI generator', () => {
  it('combines the selected namespace and identifier and emits the result', () => {
    const component = new UriGeneratorComponent();
    component.namespaces = [{ prefix: 'example', uri: 'https://example.org/' }]; component.ngOnInit();
    const emitted = spyOn(component.URIChanged, 'emit'); component.generatorForm.get('identifier').setValue('Temperature');
    component.acceptURI(); expect(component.getURI()).toBe('https://example.org/Temperature');
    expect(emitted).toHaveBeenCalledWith('https://example.org/Temperature');
  });
  it('does not overwrite an identifier the user has edited', () => {
    const updates = new Subject<string>(); const component = new UriGeneratorComponent();
    component.identifierUpdate = updates; component.ngOnInit();
    const input = component.generatorForm.get('identifier'); input.setValue('Manual'); input.markAsDirty();
    updates.next('Automatic'); expect(input.value).toBe('Manual');
  });
  it('respects disabled automatic namespace and identifier generation', () => {
    const updates = new Subject<string>(); const component = new UriGeneratorComponent();
    component.disableAutoGeneration = true; component.identifierUpdate = updates; component.namespaceUpdate = updates; component.ngOnInit();
    updates.next('Automatic'); expect(component.generatorForm.get('identifier').value).toBe('');
    expect(component.generatorForm.get('namespace').value).toBe('');
  });
  it('requires an identifier before accepting the generated form', () => {
    const component = new UriGeneratorComponent(); expect(component.generatorForm.valid).toBeFalse();
    component.generatorForm.get('identifier').setValue('Sensor'); expect(component.generatorForm.valid).toBeTrue();
  });
});
