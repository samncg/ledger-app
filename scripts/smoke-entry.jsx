// SSR smoke entry — renders the full App to a string so that missing imports,
// props or runtime typos surface as exceptions instead of a blank page.
import { renderToString } from 'react-dom/server';
import App from '../src/components/App';

export default () => renderToString(<App/>);
