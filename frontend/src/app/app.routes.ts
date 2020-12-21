import { appRoutesNames } from './app.routes.names';

export const APP_ROUTES = [
  {path: '', redirectTo: appRoutesNames.DATA_SOURCES, pathMatch: 'full'},
  {
    path: appRoutesNames.DATA_SOURCES,
    loadChildren: () => import('src/app/data-sources/data-sources.module').then(m => m.DataSourcesModule)
  },
  {
    path: appRoutesNames.SCHEMA_ANALYSIS,
    loadChildren: () => import('src/app/schema-analysis/schema-analysis.module').then(m => m.SchemaAnalysisModule)
  },
  {
    path: appRoutesNames.MODELING,
    loadChildren: () => import('src/app/modeling/modeling.module').then(m => m.ModelingModule)
  },
  {path: '**', redirectTo: ''}
];
