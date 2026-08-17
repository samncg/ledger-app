import { createRoot } from 'react-dom/client';
import App from './components/App';
import ErrorBoundary from './components/ErrorBoundary';
import './styles.css';
import './effects/typewriter'; // tab-title typewriter animation (vanilla, side-effect only)
import './effects/neko';       // desktop cursor-chasing cat (vanilla, side-effect only)

createRoot(document.getElementById('root')).render(
  <ErrorBoundary><App/></ErrorBoundary>
);
