import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { Shell } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((module) => module.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'incidents' },
      {
        path: 'incidents',
        loadComponent: () =>
          import('./features/incidents/incident-list').then((module) => module.IncidentList),
      },
      {
        path: 'incidents/new',
        canActivate: [roleGuard('ADMIN', 'DISPATCHER')],
        loadComponent: () =>
          import('./features/incidents/incident-form').then((module) => module.IncidentForm),
      },
      {
        path: 'incidents/:id',
        loadComponent: () =>
          import('./features/incidents/incident-detail').then((module) => module.IncidentDetail),
      },
      {
        path: 'map',
        loadComponent: () => import('./features/map/map-page').then((module) => module.MapPage),
      },
      {
        path: 'resources',
        loadComponent: () =>
          import('./features/resources/resource-list').then((module) => module.ResourceList),
      },
      {
        path: 'resources/new',
        canActivate: [roleGuard('ADMIN', 'DISPATCHER')],
        loadComponent: () =>
          import('./features/resources/resource-form').then((module) => module.ResourceForm),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
