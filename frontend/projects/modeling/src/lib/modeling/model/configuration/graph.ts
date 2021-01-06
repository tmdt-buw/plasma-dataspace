import { dia } from 'jointjs';
import { LinkUtil } from '../link/link-util';

export function plasmaGraph(): dia.Graph {
  const graph = new dia.Graph({
    // adjust relations verticals if something changes
    'add remove change:source change:target': (view) => {
      LinkUtil.adjustVertices(graph, view);
    }
  });
  return graph;
}
