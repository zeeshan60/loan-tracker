import { Routes } from '@angular/router';
import { TabsPage } from './tabs.page';

export const routes: Routes = [
  {
    path: 'tabs',
    component: TabsPage,
    children: [
      {
        path: 'friends',
        loadComponent: () =>
          import('../friends/friends.page').then((m) => m.FriendsPage),
      },
      {
        path: 'add-new',
        loadComponent: () =>
          import('../add-new/add-new.page').then((m) => m.AddNewPage),
      },
      {
        path: 'account',
        loadComponent: () =>
          import('../account/account.page').then((m) => m.AccountPage),
      },
      {
        path: '',
        redirectTo: '/tabs/friends',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '',
    redirectTo: '/tabs/friends',
    pathMatch: 'full',
  },
];
