import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { NotFoundComponent } from './core/not-found/not-found.component';
import { StagesComponent } from './stages/stages.component';
import { TogglesComponent } from './toggles/toggles.component';
import { ToggleTesterComponent } from './toggle-tester/toggle-tester.component';
import { SignedOutComponent } from './core/signed-out/signed-out.component';

export const routes: Routes = [
  { path: '',    component: TogglesComponent, canActivate: [authGuard] },
  { path: 'toggles',              component: TogglesComponent, canActivate: [authGuard] },
  { path: 'toggles/:toggleName', component: TogglesComponent, canActivate: [authGuard] },
  { path: 'stages',        component: StagesComponent, canActivate: [authGuard] },
  { path: 'toggle-tester', component: ToggleTesterComponent, canActivate: [authGuard] },
  { path: 'toggle-tester/:toggleName/:stage', component: ToggleTesterComponent, canActivate: [authGuard] },
  { path: 'signed-out', component: SignedOutComponent },
  { path: '**',         component: NotFoundComponent }
];
