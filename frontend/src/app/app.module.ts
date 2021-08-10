import { CommonModule, registerLocaleData } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import en from '@angular/common/locales/en';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { en_US, NZ_I18N } from 'ng-zorro-antd/i18n';
import { AppInitializerProvider } from './app-initializer.service';
import { AppComponent } from './app.component';
import { APP_ROUTES } from './app.routes';

registerLocaleData(en);

@NgModule({
  bootstrap: [AppComponent],
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    FlexLayoutModule,
    HttpClientModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatMenuModule,
    MatToolbarModule,
    RouterModule.forRoot(APP_ROUTES),
    FormsModule
  ],
  providers: [
    AppInitializerProvider,
    {provide: NZ_I18N, useValue: en_US}]
})

export class AppModule {
}
