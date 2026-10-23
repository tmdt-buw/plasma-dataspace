import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ModelingControllerService } from '@tmdt/smui';
import { NzNotificationService } from 'ng-zorro-antd/notification';
import { DataProcessingControllerService } from '../api/generated';
import { ConversionDialogComponent } from './conversion-dialog.component';
import { Util } from '../../../../projects/smui/src/lib/modeling/model/common/util';

describe('Conversion dialog', () => {
  let processing: jasmine.SpyObj<DataProcessingControllerService>;
  let modeling: jasmine.SpyObj<ModelingControllerService>;
  let notify: jasmine.SpyObj<NzNotificationService>;
  beforeEach(async () => {
    processing = jasmine.createSpyObj('DPS', ['listFiles', 'convertFile', 'uploadFile']);
    modeling = jasmine.createSpyObj('SMS', ['getCombinedModel']);
    notify = jasmine.createSpyObj('notification', ['success', 'error']);
    processing.listFiles.and.returnValue(of(['history.json', 'measurements.csv']) as any);
    modeling.getCombinedModel.and.returnValue(of({ id: 'model' }) as any);
    processing.convertFile.and.returnValue(of('<urn:s> <urn:p> "value" .') as any);
    await TestBed.configureTestingModule({ declarations: [ConversionDialogComponent], imports: [CommonModule],
      schemas: [NO_ERRORS_SCHEMA], providers: [
        { provide: DataProcessingControllerService, useValue: processing },
        { provide: ModelingControllerService, useValue: modeling },
        { provide: NzNotificationService, useValue: notify }
      ] }).compileComponents();
  });
  function render() {
    const fixture = TestBed.createComponent(ConversionDialogComponent);
    fixture.componentInstance.model = { id: 'model', dataId: 'data' } as any;
    fixture.detectChanges(); return fixture;
  }
  it('lists the available files and requires a selection', () => {
    const fixture = render();
    expect(fixture.nativeElement.querySelectorAll('.filename-item').length).toBe(2);
    expect(fixture.nativeElement.querySelector('button').disabled).toBeTrue();
    fixture.componentInstance.triggerConversion();
    expect(modeling.getCombinedModel).not.toHaveBeenCalled();
  });
  it('converts the selected file and downloads Turtle with the correct extension', () => {
    const download = spyOn(Util, 'download'); const fixture = render();
    fixture.nativeElement.querySelectorAll('.filename-item')[1].click(); fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button'); expect(button.disabled).toBeFalse(); button.click();
    expect(processing.convertFile).toHaveBeenCalledWith('data', { id: 'model' } as any, 'measurements.csv');
    expect(download).toHaveBeenCalledWith('measurements.ttl', '<urn:s> <urn:p> "value" .');
  });
  it('reports non-JSON conversion errors without throwing a second error', async () => {
    processing.convertFile.and.returnValue(throwError({ message: 'HTTP 502', error: new Blob(['Service unavailable']) }));
    const notified = new Promise<void>(resolve => notify.error.and.callFake(() => { resolve(); return {} as any; }));
    const fixture = render(); fixture.componentInstance.selectFile('history.json'); fixture.componentInstance.triggerConversion();
    await notified; expect(notify.error).toHaveBeenCalledWith('Error during conversion', 'Service unavailable');
  });
  it('reports model failures without invoking conversion', () => {
    modeling.getCombinedModel.and.returnValue(throwError({ error: { message: 'Model not found' } }));
    const fixture = render(); fixture.componentInstance.selectFile('history.json'); fixture.componentInstance.triggerConversion();
    expect(processing.convertFile).not.toHaveBeenCalled();
    expect(notify.error).toHaveBeenCalledWith('Error during conversion', 'Could not obtain semantic model: Model not found');
  });
});
