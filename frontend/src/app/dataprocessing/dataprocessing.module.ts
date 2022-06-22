import { NgModule } from '@angular/core';
import { ApiModule } from '../dataprocessing/api/generated';
import { ConversionDialogComponent } from './conversion-dialog/conversion-dialog.component';
import { FlexModule } from '@angular/flex-layout';
import { NzListModule } from 'ng-zorro-antd/list';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CommonModule } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';

@NgModule({
  declarations: [
    ConversionDialogComponent
  ],
  exports: [
  ],
    imports: [
        ApiModule,
        FlexModule,
        NzListModule,
        NzTypographyModule,
        NzUploadModule,
        NzIconModule,
        CommonModule,
        NzButtonModule,
    ]
})
export class DataprocessingModule {
}
