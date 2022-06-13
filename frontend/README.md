# PLASMA Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli).

## Services

This repository includes the PLASMA modeling UI library located under `projects/modeling`.
The library can be ported into other apps as an npm dependency `@tmdt-buw/pls-modeling`.
Please note that this library does still need the backend services to run and cannot be used
as a standalone library!

## Development

### Client generation from backend

Run `npm run gen:dms` to generate updated http-client and model from backend openapi-specs.
The DMS has to be up and running to do so.

### Development server

Run `npm run watch:dms` to build the library. The library will automatically reload if you change any of the source files.

Run `npm start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.
