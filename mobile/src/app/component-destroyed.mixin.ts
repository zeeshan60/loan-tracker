import { OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';

type Constructor<T = {}> = new (...args: any[]) => T;
export function ComponentDestroyedMixin<T extends Constructor<{}>>(Base = class {
} as T) {
  return class Mixin extends Base implements OnDestroy {
    protected componentDestroyed: Subject<void> = new Subject<void>();

    ngOnDestroy() {
      this.componentDestroyed.next();
      this.componentDestroyed.complete();
    }
  };
}
