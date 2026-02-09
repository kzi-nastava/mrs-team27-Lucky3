import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class Interceptor implements HttpInterceptor {
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const accessToken = localStorage.getItem('user');

    // 1. Check for the special 'skip' header (e.g. for Login)
    if (req.headers.get('skip')) {
      const newHeaders = req.headers.delete('skip');
      const newReq = req.clone({ headers: newHeaders });
      return next.handle(newReq);
    }

    // 2. If token exists, attach it using Bearer standard
    if (accessToken) {
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${accessToken}`
        }
      });
      return next.handle(cloned);
    }

    // 3. Fallback: Request without token
    return next.handle(req);
  }
}